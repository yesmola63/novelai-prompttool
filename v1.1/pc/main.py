import os
import sys
import hashlib
import json
import threading
import time
import socket
import shutil
import tempfile
import zipfile
import uuid
import datetime
import traceback as _traceback
import atexit
import ctypes
from flask import Flask, request, jsonify, send_from_directory
import webview

# 可选依赖：延迟加载（减小启动体积，仅在打包/加载提示词时才加载）
def _get_py7zr():
    """延迟导入 py7zr，首次调用时加载，后续直接返回"""
    if not hasattr(_get_py7zr, '_mod'):
        import py7zr as _m
        _get_py7zr._mod = _m
    return _get_py7zr._mod

def _get_pil():
    """延迟导入 PIL.Image"""
    if not hasattr(_get_pil, '_mod'):
        from PIL import Image as _m
        _get_pil._mod = _m
    return _get_pil._mod

def _get_aes():
    """延迟导入 AES 实现（优先 pycryptodomex，其次 pycryptodome）"""
    if not hasattr(_get_aes, '_mod'):
        try:
            from Cryptodome.Cipher import AES as _m
            _get_aes._mod = _m
        except ImportError:
            try:
                from Crypto.Cipher import AES as _m
                _get_aes._mod = _m
            except ImportError:
                _get_aes._mod = None
    return _get_aes._mod

def _aes_derive_key(password):
    """与安卓端一致：key = SHA-256(密码 UTF-8 字节)"""
    import hashlib
    return hashlib.sha256(password.encode('utf-8')).digest()

def _aes_encrypt_bytes(data, password):
    """AES-256-CBC 加密，返回 [16B IV][密文]，与安卓端格式完全一致"""
    mod = _get_aes()
    if mod is None:
        raise Exception('未安装 pycryptodomex，无法加密压缩，请先 pip install pycryptodomex')
    key = _aes_derive_key(password)
    iv = os.urandom(16)
    pad_len = 16 - (len(data) % 16)
    padded = data + bytes([pad_len]) * pad_len
    cipher = mod.new(key, mod.MODE_CBC, iv)
    return iv + cipher.encrypt(padded)

def _aes_decrypt_to_bytes(raw, password):
    """AES-256-CBC 解密，入参为 [16B IV][密文]，返回明文；失败抛异常"""
    mod = _get_aes()
    if mod is None:
        raise Exception('未安装 pycryptodomex，无法解密提示词，请先 pip install pycryptodomex')
    if len(raw) <= 16:
        raise ValueError('数据太短')
    iv = raw[:16]
    ct = raw[16:]
    key = _aes_derive_key(password)
    cipher = mod.new(key, mod.MODE_CBC, iv)
    pt = cipher.decrypt(ct)
    if not pt:
        raise ValueError('解密结果为空')
    pad = pt[-1]
    if pad < 1 or pad > 16:
        raise ValueError('填充无效')
    if pt[-pad:] != bytes([pad]) * pad:
        raise ValueError('填充无效')
    return pt[:-pad]

# ---------- 获取可执行文件所在目录（兼容开发环境和打包后） ----------
def get_base_dir():
    """返回可执行文件或脚本所在目录"""
    if getattr(sys, 'frozen', False):
        return os.path.dirname(sys.executable)
    else:
        return os.path.dirname(os.path.abspath(__file__))

BASE_DIR = get_base_dir()

# ---------- 日志功能 ----------
LOG_DIR = os.path.join(BASE_DIR, 'log')
os.makedirs(LOG_DIR, exist_ok=True)


class _LogFileManager:
    """按日期切换的日志文件管理器（当天复用同一文件，次日新建）"""
    def __init__(self, log_dir):
        self.log_dir = log_dir
        self._date = None
        self._fp = None

    def _ensure(self):
        today = datetime.date.today().isoformat()
        if self._date != today:
            if self._fp is not None:
                try:
                    self._fp.close()
                except Exception:
                    pass
            self._date = today
            self._fp = open(os.path.join(self.log_dir, f'{today}.log'), 'a', encoding='utf-8')
        return self._fp

    def write(self, data):
        if not data:
            return
        try:
            self._ensure().write(data)
        except Exception:
            pass

    def flush(self):
        try:
            if self._fp is not None:
                self._fp.flush()
        except Exception:
            pass


class _DailyLogWriter:
    """把原控制台流的内容同时写入控制台和日志文件（双写）"""
    def __init__(self, console_stream, file_mgr):
        self._console = console_stream
        self._file_mgr = file_mgr

    def write(self, data):
        if not data:
            return
        try:
            if self._console is not None:
                self._console.write(data)
        except Exception:
            pass
        self._file_mgr.write(data)

    def flush(self):
        try:
            if self._console is not None:
                self._console.flush()
        except Exception:
            pass
        self._file_mgr.flush()


def setup_logging():
    """初始化日志：控制台与 log/日期.log 双写；未捕获异常写入完整堆栈"""
    file_mgr = _LogFileManager(LOG_DIR)
    sys.stdout = _DailyLogWriter(sys.__stdout__, file_mgr)
    sys.stderr = _DailyLogWriter(sys.__stderr__, file_mgr)

    def _excepthook(exc_type, exc_value, exc_tb):
        text = ''.join(_traceback.format_exception(exc_type, exc_value, exc_tb))
        try:
            banner = chr(10) + '===== 未捕获异常 =====' + chr(10)
            file_mgr.write(banner + text + chr(10))
            file_mgr.flush()
            if sys.__stderr__ is not None:
                sys.__stderr__.write(banner + text + chr(10))
        except Exception:
            pass

    sys.excepthook = _excepthook
    return file_mgr


# ---------- 数据文件和图片文件夹路径（绝对路径） ----------
PROMPT_FILE = os.path.join(BASE_DIR, 'prompt.json')
UPLOAD_FOLDER = os.path.join(BASE_DIR, 'picture')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ---------- 打包/加载提示词 相关常量 ----------
TEMP_DIR = os.path.join(BASE_DIR, 'temp')          # 打包临时目录
PACK_DIR = os.path.join(BASE_DIR, 'prompt_pack')   # 默认打包保存目录
PACK_TASKS = {}                                    # 打包任务进度（task_id -> dict）
PACK_LOCK = threading.Lock()
LOAD_STATE = {'dir': None, 'valid': False, 'source': None, 'password': None}
LOAD_LOCK = threading.Lock()

# ---------- 前端轮询式接口所需状态 ----------
APP_VERSION = '1.1'
UPDATE_API_URL = 'https://novelai-prompttool-update.arisuhhome.xyz/update'
UPDATE_STATE = {
    'downloading': False, 'done': False, 'success': False,
    'downloaded': 0, 'total': 0, 'speed': 0.0, 'error': None, 'path': ''
}
UPDATE_LOCK = threading.Lock()
# 更新提示缓存文件：首次发现只记录，下次启动再提示（不依赖 WebView 的 localStorage）
UPDATE_CACHE_FILE = os.path.join(TEMP_DIR, 'update_cache.json')


def _version_tuple(v):
    s = str(v or '0').replace('-', '.').replace('+', '.')
    out = []
    for p in s.split('.'):
        try:
            out.append(int(p))
        except Exception:
            out.append(0)
    return out


def _version_gt(a, b):
    ta, tb = _version_tuple(a), _version_tuple(b)
    n = max(len(ta), len(tb))
    ta += [0] * (n - len(ta))
    tb += [0] * (n - len(tb))
    return ta > tb


def _read_update_cache():
    try:
        with open(UPDATE_CACHE_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return data if isinstance(data, dict) else None
    except Exception:
        return None


def _write_update_cache(data):
    try:
        os.makedirs(TEMP_DIR, exist_ok=True)
        tmp = UPDATE_CACHE_FILE + '.tmp'
        with open(tmp, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=1)
        os.replace(tmp, UPDATE_CACHE_FILE)
    except Exception:
        pass


def _clear_update_cache():
    try:
        if os.path.exists(UPDATE_CACHE_FILE):
            os.remove(UPDATE_CACHE_FILE)
    except Exception:
        pass

_LOAD_SELECT_RESULT = {'status': 'pending'}   # /api/load/select-file/poll 返回值
_PACK_DIR_RESULT = {'status': 'pending'}      # /api/pack/choose-dir/poll 返回值
_LOAD_TASKS = {}                              # 加载任务进度（task_id -> dict）
_LOAD_TASKS_LOCK = threading.Lock()

# ---------- prompt.json 防护 相关 ----------
BACKUP_FILE = os.path.join(TEMP_DIR, 'prompt_backup.json')      # prompt.json 备份（放在 temp 目录）
RECOVERY_FILE = os.path.join(TEMP_DIR, 'prompt_recovery.json')  # 语法损坏后提取参数的暂存文件
PROMPT_IO_LOCK = threading.Lock()                                # prompt.json 读写锁
_known_hash = None                                               # 程序认为的 prompt.json 当前 MD5

# ---------- 获取内部资源路径（用于 index.html, css, js 等） ----------
def resource_path(relative_path):
    try:
        base_path = sys._MEIPASS
    except AttributeError:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

# ---------- 检测可用端口 ----------
def find_available_port(start_port=5000):
    """从 start_port 开始，查找第一个可用的端口"""
    port = start_port
    while True:
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.bind(('127.0.0.1', port))
                return port
        except OSError:
            port += 1

# ---------- 初始化 Flask ----------
app = Flask(__name__, static_folder='.', static_url_path='')

# ---------- 数据读写函数 ----------
def load_prompts():
    if os.path.exists(PROMPT_FILE):
        with open(PROMPT_FILE, 'r', encoding='utf-8', newline='\n') as f:
            return json.load(f)
    return {}

def save_prompts(data):
    """保存 prompt.json，并同步更新备份（程序修改时备份跟着变，下次启动不会误判为人为修改）"""
    global _known_hash
    content = json.dumps(data, ensure_ascii=False, indent=2)
    with PROMPT_IO_LOCK:
        with open(PROMPT_FILE, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
        try:
            os.makedirs(TEMP_DIR, exist_ok=True)
            with open(BACKUP_FILE, 'w', encoding='utf-8', newline='\n') as f:
                f.write(content)
        except Exception:
            pass
        _known_hash = hashlib.md5(content.encode('utf-8')).hexdigest()

def has_any_prompt():
    """判断当前是否保存了任何提示词（文件不存在 / 空 / {} / 非字典 都视为没有）"""
    try:
        if not os.path.exists(PROMPT_FILE):
            return False
        with open(PROMPT_FILE, 'r', encoding='utf-8', newline='\n') as f:
            content = f.read().strip()
        if not content:
            return False
        data = json.loads(content)
        if not isinstance(data, dict):
            return False
        return any(isinstance(items, list) and len(items) > 0 for items in data.values())
    except Exception:
        return False

# ---------- prompt.json 防护：启动检查 + 运行时监控 ----------
def _safe_json_loads(content):
    """安全解析 JSON，失败返回 None"""
    try:
        return json.loads(content)
    except Exception:
        return None


def _file_md5(path):
    """计算文件 MD5（统一换行符后计算，失败返回 None）"""
    try:
        with open(path, 'r', encoding='utf-8', newline='\n') as f:
            return hashlib.md5(f.read().encode('utf-8')).hexdigest()
    except Exception:
        return None


def _repair_trailing_commas(content):
    """尝试修复最简单的语法错误：尾逗号（如 {a:1,} [1,2,]），修复成功返回文本，否则返回 None"""
    import re
    for _ in range(50):
        new = re.sub(r',\s*([}\]])', r'\1', content)
        if new == content:
            break
        content = new
    try:
        json.loads(content)
        return content
    except Exception:
        return None


def lenient_extract_prompts(content):
    """从语法损坏的 JSON 文本中尽力提取提示词参数（宽松解析）。
    返回 {group_name: [item, ...]}；提取不到返回 {}。
    """
    import re
    result = {}

    # 0) 先尝试修复尾逗号，能修好就直接用
    fixed = _repair_trailing_commas(content)
    if fixed is not None:
        try:
            data = json.loads(fixed)
            if isinstance(data, dict):
                return data
        except Exception:
            pass

    # 1) 顶层分组名："xxx" : [
    #    不用 ^ 锚定行首（压缩成一行时会失效），改为要求前面是 { 或 ,（JSON 对象键特征）
    #    并排除 item 内的数组字段（cover 等），避免误判为分组
    SKIP_GROUP_KEYS = {'cover', 'covers', 'tags', 'images', 'image'}
    group_pattern = re.compile(r'[{,]\s*"([^"]+)"\s*:\s*\[')
    group_matches = [m for m in group_pattern.finditer(content) if m.group(1) not in SKIP_GROUP_KEYS]
    groups = [m.group(1) for m in group_matches] or ['未分类']

    # 2) 字符串字段提取（支持 \" 转义）
    field_pattern = re.compile(r'"(\w+)"\s*:\s*"((?:[^"\\]|\\.)*)"')
    # 3) cover 图片数组提取
    cover_pattern = re.compile(r'"cover"\s*:\s*\[(.*?)\]', re.S)
    img_pattern = re.compile(r'"([^"]+\.(?:jpg|jpeg|png|webp|gif|bmp))"', re.I)

    # 按分组切分文本段
    segments = []
    if group_matches:
        for i, m in enumerate(group_matches):
            start = m.end()
            end = group_matches[i + 1].start() if i + 1 < len(group_matches) else len(content)
            segments.append((groups[i], content[start:end]))
    else:
        segments.append(('未分类', content))

    for gname, seg in segments:
        item = {}
        for m in field_pattern.finditer(seg):
            key = m.group(1)
            if key in ('common', 'role', 'desc', 'version', 'type', 'id'):
                item[key] = m.group(2).replace('\\"', '"').replace('\\\\', '\\')
        cm = cover_pattern.search(seg)
        if cm:
            covers = img_pattern.findall(cm.group(1))
            if covers:
                item['cover'] = covers
        if item:
            result.setdefault(gname, []).append(item)

    return result


def ensure_prompt_backup():
    """启动时检查 prompt.json 与备份。
    返回 (status, recovery_params)
    - 'ok'       正常，不需要干预
    - 'recovery' 语法错误且已提取参数，需要用户确认
    """
    global _known_hash
    os.makedirs(TEMP_DIR, exist_ok=True)

    current = None
    if os.path.exists(PROMPT_FILE):
        try:
            with open(PROMPT_FILE, 'r', encoding='utf-8', newline='\n') as f:
                current = f.read()
        except Exception:
            current = None

    # 首次运行：没有备份 → 直接备份当前内容
    if not os.path.exists(BACKUP_FILE):
        backup_content = current if current is not None else '{}'
        with open(BACKUP_FILE, 'w', encoding='utf-8', newline='\n') as f:
            f.write(backup_content)
        _known_hash = hashlib.md5(backup_content.encode('utf-8')).hexdigest()
        print('[INFO] 已创建 prompt.json 备份: ' + BACKUP_FILE)
        return 'ok', None

    # 备份存在：读取备份
    try:
        with open(BACKUP_FILE, 'r', encoding='utf-8', newline='\n') as f:
            backup_content = f.read()
    except Exception:
        backup_content = None

    # prompt.json 缺失/无法读取 → 用备份恢复
    if current is None:
        with open(PROMPT_FILE, 'w', encoding='utf-8', newline='\n') as f:
            f.write(backup_content if backup_content is not None else '{}')
        _known_hash = hashlib.md5((backup_content or '{}').encode('utf-8')).hexdigest()
        print('[WARN] prompt.json 缺失，已从备份恢复')
        return 'ok', None

    # 内容与备份一致 → 无修改，正常执行
    if current.strip() == (backup_content or '').strip():
        _known_hash = hashlib.md5(current.encode('utf-8')).hexdigest()
        return 'ok', None

    # 存在修改：先看语法
    if _safe_json_loads(current) is not None:
        # 语法正确 → 认可人为修改，当前覆盖备份
        with open(BACKUP_FILE, 'w', encoding='utf-8', newline='\n') as f:
            f.write(current)
        _known_hash = hashlib.md5(current.encode('utf-8')).hexdigest()
        print('[INFO] prompt.json 有变更且语法正确，已更新备份')
        return 'ok', None

    # 语法错误 → 尝试提取参数，交给用户确认
    params = lenient_extract_prompts(current)
    if params:
        with open(RECOVERY_FILE, 'w', encoding='utf-8', newline='\n') as f:
            json.dump(params, f, ensure_ascii=False, indent=2)
        print('[WARN] prompt.json 存在语法错误，已提取参数，等待用户确认')
        return 'recovery', params

    # 提取失败 → 回滚备份
    with open(PROMPT_FILE, 'w', encoding='utf-8', newline='\n') as f:
        f.write(backup_content if backup_content is not None else '{}')
    _known_hash = hashlib.md5((backup_content or '{}').encode('utf-8')).hexdigest()
    print('[WARN] prompt.json 语法错误且无法提取参数，已从备份回滚')
    return 'ok', None


def prompt_monitor_loop():
    """运行时监控：每 2 秒检查 prompt.json，发现外部修改立即用备份回滚"""
    global _known_hash
    while True:
        time.sleep(2)
        try:
            with PROMPT_IO_LOCK:
                h = _file_md5(PROMPT_FILE)
                if h is None:
                    continue
                if _known_hash is not None and h != _known_hash:
                    if os.path.exists(BACKUP_FILE):
                        with open(BACKUP_FILE, 'r', encoding='utf-8', newline='\n') as f:
                            backup_content = f.read()
                        with open(PROMPT_FILE, 'w', encoding='utf-8', newline='\n') as f:
                            f.write(backup_content)
                        _known_hash = hashlib.md5(backup_content.encode('utf-8')).hexdigest()
                        print('[WARN] 检测到 prompt.json 被外部修改，已自动回滚备份')
        except Exception as e:
            print('[WARN] prompt 监控线程异常: ' + str(e))


def start_prompt_monitor():
    """启动 prompt.json 运行时监控线程（守护线程）"""
    t = threading.Thread(target=prompt_monitor_loop, daemon=True, name='prompt-monitor')
    t.start()
    print('[INFO] prompt.json 运行时监控已启动（每 2 秒检查）')

# ====================================================================
# 路由顺序：首页 -> 图片 -> API -> 通用静态（必须放在最后）
# ====================================================================

@app.route('/')
def index():
    return send_from_directory(resource_path('.'), 'index.html')

@app.route('/picture/<filename>')
def get_picture(filename):
    return send_from_directory(UPLOAD_FOLDER, filename)

# ---------- API 路由 ----------
@app.route('/api/prompts', methods=['GET'])
def get_prompts():
    return jsonify(load_prompts())

@app.route('/api/prompts/check-empty', methods=['GET'])
def check_prompts_empty():
    """检查当前是否保存了任何提示词（供打包前拦截）"""
    return jsonify({'empty': not has_any_prompt()})

@app.route('/api/prompts', methods=['POST'])
def add_prompt():
    req = request.json
    group = req.get('group')
    item = req.get('item')
    if not group or not item:
        return jsonify({'error': '缺少分组或提示词数据'}), 400

    prompts = load_prompts()
    if group not in prompts:
        prompts[group] = []

    for existing in prompts[group]:
        if existing.get('role') == item.get('role') and existing.get('common') == item.get('common'):
            return jsonify({'error': '提示词已存在'}), 409

    prompts[group].append(item)
    save_prompts(prompts)
    return jsonify({'message': '添加成功'}), 201

@app.route('/api/prompts', methods=['PUT'])
def update_prompt():
    req = request.json
    original_group = req.get('originalGroup')
    original_role = req.get('originalRole')
    original_common = req.get('originalCommon')
    new_group = req.get('newGroup')
    new_item = req.get('newItem')

    if not all([original_group, original_role, original_common, new_group, new_item]):
        return jsonify({'error': '缺少必要参数'}), 400

    prompts = load_prompts()
    if original_group not in prompts:
        return jsonify({'error': '原始分组不存在'}), 404

    original_item = None
    for item in prompts.get(original_group, []):
        if item.get('role') == original_role and item.get('common') == original_common:
            original_item = item
            break
    if not original_item:
        return jsonify({'error': '未找到原始提示词'}), 404

    # 删除原项
    prompts[original_group] = [
        item for item in prompts.get(original_group, [])
        if not (item.get('role') == original_role and item.get('common') == original_common)
    ]
    if not prompts[original_group]:
        del prompts[original_group]

    # 检查新组冲突
    if new_group not in prompts:
        prompts[new_group] = []
    for existing in prompts[new_group]:
        if existing.get('role') == new_item.get('role') and existing.get('common') == new_item.get('common'):
            # 恢复原项
            if original_group not in prompts:
                prompts[original_group] = []
            prompts[original_group].append(original_item)
            save_prompts(prompts)
            return jsonify({'error': '更新后与同组其他提示词冲突'}), 409

    prompts[new_group].append(new_item)
    save_prompts(prompts)
    return jsonify({'message': '更新成功'})

@app.route('/api/prompts', methods=['DELETE'])
def delete_prompt():
    item_id = request.args.get('id')
    group = request.args.get('group')
    role = request.args.get('role')
    common = request.args.get('common')

    prompts = load_prompts()

    # 优先按 id 删除（新版前端使用 id 定位）
    if item_id:
        removed = False
        for g in list(prompts.keys()):
            kept = [it for it in prompts[g] if it.get('id') != item_id]
            if len(kept) != len(prompts[g]):
                removed = True
                prompts[g] = kept
                if not prompts[g]:
                    del prompts[g]
                break
        if not removed:
            return jsonify({'error': '未找到该提示词'}), 404
        save_prompts(prompts)
        return jsonify({'message': '删除成功'})

    if not group or not role or not common:
        return jsonify({'error': '缺少参数'}), 400

    if group not in prompts:
        return jsonify({'error': '分组不存在'}), 404

    original_len = len(prompts[group])
    prompts[group] = [
        item for item in prompts[group]
        if not (item.get('role') == role and item.get('common') == common)
    ]
    if len(prompts[group]) == original_len:
        return jsonify({'error': '未找到该提示词'}), 404

    if not prompts[group]:
        del prompts[group]

    save_prompts(prompts)
    return jsonify({'message': '删除成功'})

@app.route('/api/upload-images', methods=['POST'])
def upload_images():
    if 'files' not in request.files:
        return jsonify({'error': '未上传文件'}), 400
    files = request.files.getlist('files')
    if not files:
        return jsonify({'error': '文件列表为空'}), 400

    filenames = []
    for file in files:
        if file.filename == '':
            continue
        file_data = file.read()
        sha1 = hashlib.sha1(file_data).hexdigest()
        ext = os.path.splitext(file.filename)[1] or '.jpg'
        filename = sha1 + ext
        save_path = os.path.join(UPLOAD_FOLDER, filename)
        if not os.path.exists(save_path):
            with open(save_path, 'wb') as f:
                f.write(file_data)
        filenames.append(filename)

    return jsonify({'filenames': filenames})

@app.route('/api/images', methods=['DELETE'])
def delete_images():
    data = request.json
    filenames = data.get('filenames', [])
    if not filenames:
        return jsonify({'error': '未指定文件名'}), 400

    deleted = []
    for name in filenames:
        filepath = os.path.join(UPLOAD_FOLDER, name)
        if os.path.exists(filepath):
            os.remove(filepath)
            deleted.append(name)
    return jsonify({'deleted': deleted})

@app.route('/api/download-image', methods=['POST'])
def download_image():
    data = request.json
    filename = data.get('filename')
    if not filename:
        return jsonify({'error': '缺少文件名'}), 400

    src_path = os.path.join(UPLOAD_FOLDER, filename)
    if not os.path.exists(src_path):
        return jsonify({'error': '图片不存在'}), 404

    # 使用 webview 弹出保存对话框
    try:
        import webview
        window = webview.windows[0]
        if window is None:
            return jsonify({'error': '窗口未初始化'}), 500

        result = window.create_file_dialog(
            webview.SAVE_DIALOG,
            allow_multiple=False,
            file_types=('图片文件 (*.jpg;*.jpeg;*.png;*.gif;*.bmp;*.webp)', '所有文件 (*.*)'),
            save_filename=filename
        )
        if not result:  # 用户取消
            return jsonify({'cancelled': True})

        # 兼容不同 pywebview 版本的返回值（可能是字符串或单元素元组）
        dest_path = result[0] if isinstance(result, (tuple, list)) else result
        import shutil
        shutil.copy2(src_path, dest_path)
        return jsonify({'message': '保存成功'})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/prompts/replace', methods=['PUT'])
def replace_prompts():
    """整体替换提示词数据（用于批量更新 UUID）"""
    data = request.json
    if not isinstance(data, dict):
        return jsonify({'error': '数据格式错误'}), 400
    save_prompts(data)
    return jsonify({'message': '更新成功'})


@app.route('/api/prompt/recovery-check', methods=['GET'])
def prompt_recovery_check():
    """检查是否有待用户确认的恢复数据（prompt.json 语法错误提取结果）"""
    if os.path.exists(RECOVERY_FILE):
        try:
            with open(RECOVERY_FILE, 'r', encoding='utf-8') as f:
                params = json.load(f)
            return jsonify({'needRecovery': True, 'params': params})
        except Exception:
            pass
    return jsonify({'needRecovery': False, 'params': None})


@app.route('/api/prompt/recover', methods=['POST'])
def prompt_recover():
    """用户确认后，用数据覆盖 prompt.json 中原本错误的地方"""
    req = request.json or {}
    data = req.get('data')
    if not isinstance(data, dict) or not data:
        return jsonify({'error': '数据格式错误'}), 400
    try:
        save_prompts(data)
    except Exception as e:
        return jsonify({'error': '保存失败: ' + str(e)}), 500
    if os.path.exists(RECOVERY_FILE):
        try:
            os.remove(RECOVERY_FILE)
        except Exception:
            pass
    return jsonify({'message': '修复成功'})

# ====================================================================
# 打包提示词 / 加载提示词 功能
# ====================================================================

def ensure_list(v):
    """把值安全转成列表"""
    if not v:
        return []
    return v if isinstance(v, list) else [v]


def collect_prompts(selected_ids=None):
    """
    根据选中的提示词 id 列表重组提示词数据。
    返回 (prompt_data, image_set)
    - selected_ids 为空/None 时打包全部
    - 图片集合包含所有被选中提示词引用的图片（含同时被未选中提示词引用的图片）
    """
    prompts = load_prompts()
    image_set = set()
    if not selected_ids:
        for group, items in prompts.items():
            for it in items:
                for f in ensure_list(it.get('cover')):
                    image_set.add(f)
        return prompts, image_set

    selected = set(selected_ids)
    new_data = {}
    for group, items in prompts.items():
        keep = []
        for it in items:
            if it.get('id') in selected:
                keep.append(it)
                for f in ensure_list(it.get('cover')):
                    image_set.add(f)
        if keep:
            new_data[group] = keep
    return new_data, image_set


def _zip_write(zf, base_dir, skip_names=None):
    """把 base_dir 下所有文件写入 zip，skip_names 用于排除临时压缩文件"""
    skip_names = skip_names or set()
    for root, dirs, files in os.walk(base_dir):
        dirs[:] = [d for d in dirs if d not in skip_names]
        for name in files:
            if name in skip_names:
                continue
            full = os.path.join(root, name)
            rel = os.path.relpath(full, base_dir).replace('\\', '/')
            zf.write(full, rel)
    # 确保 picture 目录条目存在（空图片文件夹也能被校验）
    if not os.listdir(os.path.join(base_dir, 'picture')):
        zf.writestr('picture/', '')


def _update_task(task_id, text='', done=False, success=False, message='', error=''):
    with PACK_LOCK:
        PACK_TASKS[task_id] = {
            'text': text,
            'done': done,
            'success': success,
            'message': message,
            'error': error,
        }


def pack_worker(task_id, req):
    """后台打包线程"""
    try:
        filename = (req.get('filename') or 'prompt_pack').strip() or 'prompt_pack'
        save_dir = (req.get('savePath') or '').strip() or PACK_DIR
        need_password = bool(req.get('needPassword'))
        password = req.get('password') or ''
        selected_ids = req.get('selectedIds')
        image_mode = req.get('imageMode') or 'none'   # none | webp | zip
        webp_lossless = bool(req.get('webpLossless'))
        webp_quality = int(req.get('webpQuality') or 80)
        zip_mode = req.get('zipMode') or 'standard'   # standard | max | extreme

        # 1. 重组提示词 + 收集图片
        prompt_data, image_set = collect_prompts(selected_ids)

        # 1.5 空提示词防御（后端兜底）
        if not prompt_data:
            raise Exception('没有可打包的提示词')

        # 2. 准备 temp 目录
        if os.path.exists(TEMP_DIR):
            shutil.rmtree(TEMP_DIR, ignore_errors=True)
        pic_temp = os.path.join(TEMP_DIR, 'picture')
        os.makedirs(pic_temp, exist_ok=True)

        with open(os.path.join(TEMP_DIR, 'prompt.json'), 'w', encoding='utf-8') as f:
            json.dump(prompt_data, f, ensure_ascii=False, indent=2)

        # 3. 复制图片
        _update_task(task_id, '正在准备图片文件...')
        for img in image_set:
            src = os.path.join(UPLOAD_FOLDER, img)
            if os.path.exists(src):
                shutil.copy2(src, os.path.join(pic_temp, img))

        # 4. webp 压缩（仅 png）
        if image_mode == 'webp':
            PIL = _get_pil()
            if PIL is None:
                raise Exception('未安装 Pillow，无法压缩图片，请先 pip install Pillow')
            pngs = [f for f in os.listdir(pic_temp) if f.lower().endswith('.png')]
            total = len(pngs)
            mapping = {}
            for i, name in enumerate(pngs, 1):
                _update_task(task_id, f'正在压缩图片: {i}/{total}')
                path = os.path.join(pic_temp, name)
                try:
                    im = PIL.open(path)
                    if im.mode in ('P', 'LA') or 'transparency' in im.info:
                        im = im.convert('RGBA')
                    elif im.mode != 'RGB':
                        im = im.convert('RGB')
                    webp_name = os.path.splitext(name)[0] + '.webp'
                    if webp_lossless:
                        im.save(os.path.join(pic_temp, webp_name), 'WEBP', lossless=True)
                    else:
                        im.save(os.path.join(pic_temp, webp_name), 'WEBP', quality=webp_quality)
                    im.close()
                    os.remove(path)
                    mapping[name] = webp_name
                except Exception as e:
                    print(f'[WARN] webp 压缩失败 {name}: {e}')
            # 同步更新 prompt.json 里的图片引用
            if mapping:
                for group, items in prompt_data.items():
                    for it in items:
                        it['cover'] = [mapping.get(c, c) for c in ensure_list(it.get('cover'))]
                with open(os.path.join(TEMP_DIR, 'prompt.json'), 'w', encoding='utf-8') as f:
                    json.dump(prompt_data, f, ensure_ascii=False, indent=2)

        # 5. 压缩打包
        os.makedirs(save_dir, exist_ok=True)
        bundle_path = os.path.join(save_dir, filename + '.bundle')
        # 临时压缩文件放到系统临时目录，避免混进 temp 里
        tmp_arch = os.path.join(tempfile.gettempdir(), f'pack_{task_id}.tmp')

        if need_password:
            aes_mod = _get_aes()
            if aes_mod is None:
                raise Exception('未安装 pycryptodomex，无法加密压缩，请先 pip install pycryptodomex')
            _update_task(task_id, '正在以AES格式加密压缩提示词...')
            if os.path.exists(tmp_arch):
                os.remove(tmp_arch)
            # 先打 zip（deflate 压缩，与安卓端一致）
            with zipfile.ZipFile(tmp_arch, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
                _zip_write(z, TEMP_DIR, skip_names={'pack_' + task_id + '.tmp'})
            # AES-256-CBC 加密：[16B IV][密文]，key=SHA-256(密码)
            with open(tmp_arch, 'rb') as f:
                zip_bytes = f.read()
            encrypted = _aes_encrypt_bytes(zip_bytes, password)
            with open(bundle_path, 'wb') as f:
                f.write(encrypted)
            if os.path.exists(tmp_arch):
                os.remove(tmp_arch)
        else:
            if image_mode == 'zip':
                # 用压缩包压缩（deflate/lzma）
                if zip_mode == 'extreme':
                    _update_task(task_id, '正在以极限压缩模式压缩提示词...')
                    with zipfile.ZipFile(tmp_arch, 'w', zipfile.ZIP_LZMA) as z:
                        _zip_write(z, TEMP_DIR, skip_names={'pack_' + task_id + '.tmp'})
                else:
                    level = 9 if zip_mode == 'max' else 6
                    mode_name = '最大' if zip_mode == 'max' else '标准'
                    _update_task(task_id, f'正在以{mode_name}压缩模式压缩提示词...')
                    with zipfile.ZipFile(tmp_arch, 'w', zipfile.ZIP_DEFLATED, compresslevel=level) as z:
                        _zip_write(z, TEMP_DIR, skip_names={'pack_' + task_id + '.tmp'})
            else:
                # 不压缩图片：zip 仅存储模式
                _update_task(task_id, '正在打包提示词...')
                with zipfile.ZipFile(tmp_arch, 'w', zipfile.ZIP_STORED) as z:
                    _zip_write(z, TEMP_DIR, skip_names={'pack_' + task_id + '.tmp'})

        # 重命名为 .bundle（加密分支已直接写 bundle_path，跳过）
        if not need_password:
            shutil.move(tmp_arch, bundle_path)

        # 6. 清理 temp
        shutil.rmtree(TEMP_DIR, ignore_errors=True)
        _update_task(task_id, done=True, success=True, message=bundle_path)
    except Exception as e:
        shutil.rmtree(TEMP_DIR, ignore_errors=True)
        _update_task(task_id, done=True, success=False, error=str(e))


@app.route('/api/pack', methods=['POST'])
def pack_prompts():
    req = request.json or {}
    task_id = uuid.uuid4().hex
    _update_task(task_id, '正在初始化...')
    t = threading.Thread(target=pack_worker, args=(task_id, req), daemon=True)
    t.start()
    return jsonify({'task_id': task_id})


@app.route('/api/pack/progress', methods=['GET'])
def pack_progress():
    task_id = request.args.get('task_id')
    if not task_id:
        return jsonify({'error': '缺少 task_id'}), 400
    with PACK_LOCK:
        info = PACK_TASKS.get(task_id)
    if not info:
        return jsonify({'error': '任务不存在'}), 404
    return jsonify(info)


@app.route('/api/pack/choose-dir', methods=['POST'])
def pack_choose_dir():
    """弹出文件夹选择对话框（用于选择打包保存路径）"""
    global _PACK_DIR_RESULT
    try:
        window = webview.windows[0]
        if window is None:
            return jsonify({'error': '窗口未初始化'}), 500
        result = window.create_file_dialog(webview.FOLDER_DIALOG)
        if not result:
            _PACK_DIR_RESULT = {'cancelled': True}
            return jsonify({'cancelled': True})
        path = result[0] if isinstance(result, (tuple, list)) else result
        _PACK_DIR_RESULT = {'path': path}
        return jsonify({'path': path})
    except Exception as e:
        _PACK_DIR_RESULT = {'error': str(e)}
        return jsonify({'error': str(e)}), 500


@app.route('/api/pack/choose-dir/poll', methods=['GET'])
def pack_choose_dir_poll():
    """前端轮询取目录选择结果"""
    return jsonify(_PACK_DIR_RESULT or {'status': 'pending'})


@app.route('/api/pack/check-filename', methods=['POST'])
def pack_check_filename():
    """打包前检查同名文件是否已存在"""
    data = request.json or {}
    filename = (data.get('filename') or 'prompt').strip() or 'prompt'
    save_path = (data.get('savePath') or '').strip() or PACK_DIR
    target = os.path.join(save_path, filename + '.bundle')
    return jsonify({'exists': os.path.exists(target), 'path': target})


# ---------- 加载提示词 ----------

def _is_7z_file(path):
    """通过文件头判断是否为 7z 归档"""
    try:
        with open(path, 'rb') as f:
            return f.read(6) == b'7z\xbc\xaf\x27\x1c'
    except Exception:
        return False


def _is_zip_file(path):
    """通过文件头判断是否为 zip 归档"""
    try:
        with open(path, 'rb') as f:
            return f.read(4) == b'PK\x03\x04'
    except Exception:
        return False


def try_extract(cache_dir, password=None):
    """
    尝试把 LOAD_STATE['source'] 解压到 cache_dir。
    返回 (ok, need_password, error)
    """
    src = LOAD_STATE.get('source')
    if not src or not os.path.exists(src):
        return False, False, '源文件不存在'

    # 先清理缓存目录
    for name in os.listdir(cache_dir):
        p = os.path.join(cache_dir, name)
        if os.path.isfile(p):
            os.remove(p)
        else:
            shutil.rmtree(p, ignore_errors=True)

    # 按文件头判断格式，避免 7z / zip 相互干扰
    if _is_7z_file(src):
        py7zr = _get_py7zr()
        if py7zr is not None:
            tmp7z = os.path.join(cache_dir, 'bundle.7z')
            shutil.copy2(src, tmp7z)
            try:
                with py7zr.SevenZipFile(tmp7z, 'r', password=password) as z:
                    names = z.getnames() or []
                    # 过滤掉 "." 及目录条目，兼容旧版 writeall(arcname='.') 打包的文件
                    targets = [n for n in names if n and n != '.' and not n.endswith('/')]
                    if targets:
                        z.extract(path=cache_dir, targets=targets)
                    else:
                        z.extractall(path=cache_dir)
                if os.path.exists(os.path.join(cache_dir, 'prompt.json')):
                    return True, False, ''
                return False, False, '格式不正确'
            except py7zr.exceptions.PasswordRequired:
                return False, True, '需要密码'
            except py7zr.exceptions.Bad7zFile:
                pass  # 不是合法 7z，继续尝试 zip
            except (py7zr.exceptions.CrcError, py7zr.exceptions.DecompressionError):
                # 密码错误导致校验失败
                return False, True, '需要密码'
            except Exception as e:
                # 密码错误时 py7zr 1.x 会抛 _lzma.LZMAError（Corrupt input data），
                # 只要已提供密码就视为密码错误，而不是文件损坏
                if password:
                    return False, True, '需要密码'
                msg = str(e).lower()
                if 'password' in msg or 'encrypted' in msg:
                    return False, True, '需要密码'
            pass  # 继续尝试 zip

    if _is_zip_file(src):
        # 尝试 zip
        tmpzip = os.path.join(cache_dir, 'bundle.zip')
        shutil.copy2(src, tmpzip)
        try:
            with zipfile.ZipFile(tmpzip, 'r') as z:
                z.extractall(path=cache_dir)
            if os.path.exists(os.path.join(cache_dir, 'prompt.json')):
                return True, False, ''
            return False, False, '格式不正确'
        except Exception as e:
            msg = str(e).lower()
            if 'encrypted' in msg or 'password' in msg:
                return False, True, '需要密码'
            return False, False, '无法解压'

    # AES-256-CBC 加密格式（与安卓端互通）：[16B IV][密文]，密文长度应为 16 的倍数
    size = os.path.getsize(src)
    if size > 16 and (size - 16) % 16 == 0:
        if password:
            try:
                with open(src, 'rb') as f:
                    raw = f.read()
                decrypted = _aes_decrypt_to_bytes(raw, password)
                if not decrypted.startswith(b'PK\x03\x04'):
                    return False, True, '需要密码'  # 密码错误，解密结果不是 zip
                tmpzip = os.path.join(cache_dir, 'bundle_aes.zip')
                with open(tmpzip, 'wb') as f:
                    f.write(decrypted)
                with zipfile.ZipFile(tmpzip, 'r') as z:
                    z.extractall(path=cache_dir)
                if os.path.exists(os.path.join(cache_dir, 'prompt.json')):
                    return True, False, ''
                return False, False, '格式不正确'
            except Exception:
                return False, True, '需要密码'  # 密码错误或非 AES 文件
        return False, True, '需要密码'

    # 无法识别的格式
    return False, False, '无法识别的格式'


@app.route('/api/load/select-file', methods=['POST'])
def load_select_file():
    """弹出文件选择对话框，选择 .bundle 提示词文件并尝试解压校验"""
    global _LOAD_SELECT_RESULT
    try:
        window = webview.windows[0]
        if window is None:
            return jsonify({'error': '窗口未初始化'}), 500
        result = window.create_file_dialog(
            webview.OPEN_DIALOG,
            allow_multiple=False,
            file_types=('提示词文件 (*.bundle)', '所有文件 (*.*)')
        )
        if not result:
            _LOAD_SELECT_RESULT = {'cancelled': True}
            return jsonify({'cancelled': True})
        src = result[0] if isinstance(result, (tuple, list)) else result
        if not os.path.exists(src):
            _LOAD_SELECT_RESULT = {'error': '文件不存在'}
            return jsonify({'error': '文件不存在'}), 400

        with LOAD_LOCK:
            # 清理旧缓存
            if LOAD_STATE.get('dir') and os.path.exists(LOAD_STATE['dir']):
                shutil.rmtree(LOAD_STATE['dir'], ignore_errors=True)
            cache_dir = tempfile.mkdtemp(prefix='novelai_load_')
            LOAD_STATE['source'] = src
            LOAD_STATE['dir'] = cache_dir
            LOAD_STATE['valid'] = False
            LOAD_STATE['password'] = None

            ok, need_pwd, err = try_extract(cache_dir)
            if ok:
                LOAD_STATE['valid'] = True
                _LOAD_SELECT_RESULT = {'valid': True, 'needPassword': False,
                                       'filename': os.path.basename(src)}
                return jsonify({'valid': True, 'needPassword': False,
                                'filename': os.path.basename(src)})
            if need_pwd:
                _LOAD_SELECT_RESULT = {'valid': False, 'needPassword': True,
                                       'filename': os.path.basename(src)}
                return jsonify({'valid': False, 'needPassword': True,
                                'filename': os.path.basename(src)})
            _LOAD_SELECT_RESULT = {'valid': False, 'needPassword': False,
                                   'error': '该文件可能不是提示词文件或者提示词文件损坏'}
            return jsonify({'valid': False, 'needPassword': False,
                            'error': '该文件可能不是提示词文件或者提示词文件损坏'})
    except Exception as e:
        _LOAD_SELECT_RESULT = {'error': str(e)}
        return jsonify({'error': str(e)}), 500


@app.route('/api/load/unlock', methods=['POST'])
def load_unlock():
    """使用密码解锁并校验提示词文件"""
    req = request.json or {}
    password = req.get('password') or ''
    with LOAD_LOCK:
        if not LOAD_STATE.get('source') or not LOAD_STATE.get('dir'):
            return jsonify({'error': '请先选择提示词文件'}), 400
        cache_dir = LOAD_STATE['dir']
        ok, need_pwd, err = try_extract(cache_dir, password=password)
        if ok:
            LOAD_STATE['valid'] = True
            LOAD_STATE['password'] = password
            return jsonify({'valid': True})
        if need_pwd:
            return jsonify({'valid': False, 'needPassword': True})
        return jsonify({'valid': False, 'needPassword': False,
                        'error': '该文件可能不是提示词文件或者提示词文件损坏'})


def merge_prompts(old_data, new_data):
    """合并提示词：分组重名则并入已有分组；指令(role+common)重名则跳过"""
    result = dict(old_data)
    for group, items in new_data.items():
        if group not in result:
            result[group] = []
        for item in items:
            dup = any(
                ex.get('role') == item.get('role') and ex.get('common') == item.get('common')
                for ex in result[group]
            )
            if not dup:
                result[group].append(item)
    # 清理空分组
    for g in list(result.keys()):
        if not result[g]:
            del result[g]
    return result


@app.route('/api/load/apply', methods=['POST'])
def load_apply():
    """把已校验的提示词应用到当前程序（合并或替换）

    新版前端会先拿到 task_id，再轮询 /api/load/progress 获取结果，
    因此这里改为后台线程执行、立即返回 task_id。
    """
    req = request.json or {}
    mode = req.get('mode')  # merge | replace
    with LOAD_LOCK:
        if not LOAD_STATE.get('valid') or not LOAD_STATE.get('dir'):
            return jsonify({'error': '请先选择并校验提示词文件'}), 400
        cache_dir = LOAD_STATE['dir']
        new_prompt_path = os.path.join(cache_dir, 'prompt.json')
        if not os.path.exists(new_prompt_path):
            return jsonify({'error': '缓存中缺少 prompt.json'}), 400
        try:
            with open(new_prompt_path, 'r', encoding='utf-8') as f:
                new_data = json.load(f)
        except Exception as e:
            return jsonify({'error': f'prompt.json 解析失败: {e}'}), 400

    task_id = str(uuid.uuid4())
    with _LOAD_TASKS_LOCK:
        _LOAD_TASKS[task_id] = {'done': False, 'success': False, 'text': '正在应用提示词…'}
    threading.Thread(target=_apply_load_worker,
                     args=(task_id, mode, cache_dir, new_data), daemon=True).start()
    return jsonify({'task_id': task_id})


def _apply_load_worker(task_id, mode, cache_dir, new_data):
    def finish(success, error=None, message=None):
        with _LOAD_TASKS_LOCK:
            res = {'done': True, 'success': success}
            if error:
                res['error'] = error
            if message:
                res['message'] = message
                res['mode'] = mode
            _LOAD_TASKS[task_id] = res

    try:
        src_pic = os.path.join(cache_dir, 'picture')

        if mode == 'replace':
            # 删除当前所有图片
            for name in os.listdir(UPLOAD_FOLDER):
                p = os.path.join(UPLOAD_FOLDER, name)
                if os.path.isfile(p):
                    os.remove(p)
            # 复制新图片（同名覆盖）
            if os.path.exists(src_pic):
                for name in os.listdir(src_pic):
                    s = os.path.join(src_pic, name)
                    if os.path.isfile(s):
                        shutil.copy2(s, os.path.join(UPLOAD_FOLDER, name))
            save_prompts(new_data)
        else:
            # merge：图片同名不覆盖（指向同一个图片）
            if os.path.exists(src_pic):
                for name in os.listdir(src_pic):
                    s = os.path.join(src_pic, name)
                    if os.path.isfile(s):
                        dst = os.path.join(UPLOAD_FOLDER, name)
                        if not os.path.exists(dst):
                            shutil.copy2(s, dst)
            merged = merge_prompts(load_prompts(), new_data)
            save_prompts(merged)

        # 清理缓存
        shutil.rmtree(cache_dir, ignore_errors=True)
        with LOAD_LOCK:
            LOAD_STATE.update({'dir': None, 'valid': False, 'source': None, 'password': None})
        finish(True, message='加载成功')
    except Exception as e:
        _traceback.print_exc()
        finish(False, error=str(e))


# ====================================================================
# 应用信息 / 应用内更新 / 图片另存 / 加载轮询
# ====================================================================

@app.route('/api/app-info', methods=['GET'])
def app_info():
    """返回当前应用版本（供前端比对更新）"""
    return jsonify({'version': APP_VERSION, 'versionCode': 0})


@app.route('/api/update/check', methods=['GET'])
def update_check():
    """代理请求远程更新接口，并维护"待提示"的缓存文件"""
    import urllib.request
    try:
        req = urllib.request.Request(
            UPDATE_API_URL,
            headers={'Accept': 'application/json', 'User-Agent': 'PromptTool-PC'}
        )
        with urllib.request.urlopen(req, timeout=8) as resp:
            raw = resp.read().decode('utf-8')
        remote = json.loads(raw)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

    # 缓存维护：
    #   有新版本且与缓存里的版本不同 → 记录并标记 pending（本次不提示，下次启动提示）
    #   没有新版本 → 删除上次的缓存文件
    try:
        remote_ver = str(remote.get('version') or '')
        if remote_ver and _version_gt(remote_ver, APP_VERSION):
            cache = _read_update_cache()
            if not cache or str(cache.get('version') or '') != remote_ver:
                _write_update_cache({
                    'version': remote_ver,
                    'changelog': remote.get('changelog') or '',
                    'downloads': remote.get('downloads') or {},
                    'published_at': remote.get('published_at') or '',
                    'snooze_date': '',
                    'cached_at': time.time(),
                })
        else:
            _clear_update_cache()
    except Exception:
        pass

    return jsonify(remote)


@app.route('/api/update/pending', methods=['GET'])
def update_pending():
    """返回缓存的更新（检测到后每次启动都提示；勾了"当天不再显示"则当天不提示）"""
    cache = _read_update_cache()
    if not cache or not cache.get('version'):
        return jsonify({'show': False})
    if not _version_gt(str(cache.get('version')), APP_VERSION):
        _clear_update_cache()
        return jsonify({'show': False})
    # 当天已选择"不再显示" → 今天不弹，明天恢复
    if str(cache.get('snooze_date') or '') == datetime.date.today().isoformat():
        return jsonify({'show': False})
    return jsonify({
        'show': True,
        'info': {
            'version': cache.get('version'),
            'changelog': cache.get('changelog', ''),
            'downloads': cache.get('downloads', {}),
            'published_at': cache.get('published_at', ''),
        }
    })


@app.route('/api/update/snooze', methods=['POST'])
def update_snooze():
    """记录"当天不再显示"（只影响今天）"""
    cache = _read_update_cache()
    if cache and cache.get('version'):
        cache['snooze_date'] = datetime.date.today().isoformat()
        _write_update_cache(cache)
    return jsonify({'ok': True})


@app.route('/api/update/download', methods=['POST'])
def update_download():
    """后台下载新版安装包，进度由 /api/update/progress 提供"""
    data = request.json or {}
    url = (data.get('url') or '').strip()
    if not url.startswith(('http://', 'https://')):
        return jsonify({'error': 'invalid_url'}), 400
    with UPDATE_LOCK:
        if UPDATE_STATE['downloading']:
            return jsonify({'error': 'already_downloading'}), 409
        UPDATE_STATE.update({
            'downloading': True, 'done': False, 'success': False,
            'downloaded': 0, 'total': 0, 'speed': 0.0, 'error': None, 'path': ''
        })
    threading.Thread(target=_download_update_worker, args=(url,), daemon=True).start()
    return jsonify({'started': True})


def _download_update_worker(url):
    import urllib.request
    try:
        save_dir = os.path.join(tempfile.gettempdir(), 'NovelAIPromptToolUpdate')
        os.makedirs(save_dir, exist_ok=True)
        name = url.split('?')[0].rstrip('/').split('/')[-1] or 'prompttool-update.bin'
        dest = os.path.join(save_dir, name)

        req = urllib.request.Request(url, headers={'User-Agent': 'PromptTool-PC'})
        with urllib.request.urlopen(req, timeout=30) as resp:
            try:
                UPDATE_STATE['total'] = int(resp.headers.get('Content-Length') or 0)
            except Exception:
                UPDATE_STATE['total'] = 0
            downloaded = 0
            last_tick = time.time()
            last_bytes = 0
            with open(dest, 'wb') as f:
                while True:
                    chunk = resp.read(64 * 1024)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    UPDATE_STATE['downloaded'] = downloaded
                    now = time.time()
                    if now - last_tick >= 0.5:
                        UPDATE_STATE['speed'] = (downloaded - last_bytes) / (now - last_tick)
                        last_bytes = downloaded
                        last_tick = now

        UPDATE_STATE['speed'] = 0.0
        if not os.path.exists(dest) or os.path.getsize(dest) <= 0:
            raise Exception('empty_file')
        UPDATE_STATE['downloaded'] = os.path.getsize(dest)
        if not UPDATE_STATE['total']:
            UPDATE_STATE['total'] = UPDATE_STATE['downloaded']
        UPDATE_STATE['path'] = dest
        UPDATE_STATE['done'] = True
        UPDATE_STATE['success'] = True
        print(f'[INFO] 更新包下载完成: {dest}')
    except Exception as e:
        UPDATE_STATE['error'] = str(e)
        UPDATE_STATE['done'] = True
        UPDATE_STATE['success'] = False
        print(f'[ERROR] 更新包下载失败: {e}')
    finally:
        UPDATE_STATE['downloading'] = False


@app.route('/api/update/progress', methods=['GET'])
def update_progress():
    return jsonify(dict(UPDATE_STATE))


@app.route('/api/update/install', methods=['POST'])
def update_install():
    """下载完成后用系统默认程序打开安装包（exe/zip 等）"""
    path = UPDATE_STATE.get('path') or ''
    if not path or not os.path.exists(path):
        return jsonify({'error': 'file_not_found'}), 400
    try:
        if os.name == 'nt':
            os.startfile(path)  # noqa: 仅 Windows 可用
        else:
            import subprocess
            subprocess.Popen(['xdg-open', path])
        return jsonify({'installing': True, 'path': path})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/save-image', methods=['POST'])
def save_image_alias():
    """兼容新版前端的图片另存接口（等价于 /api/download-image）"""
    return download_image()


@app.route('/api/load/select-file/poll', methods=['GET'])
def load_select_file_poll():
    """前端轮询取文件选择结果"""
    return jsonify(_LOAD_SELECT_RESULT or {'status': 'pending'})


@app.route('/api/load/progress', methods=['GET'])
def load_progress():
    """查询加载任务进度"""
    task_id = request.args.get('task_id') or ''
    with _LOAD_TASKS_LOCK:
        info = _LOAD_TASKS.get(task_id)
    if info is None:
        return jsonify({'error': 'unknown_task'}), 404
    return jsonify(info)


# ---------- 通用静态路由（必须放在最后，仅处理 .css .js 等） ----------
@app.route('/<path:filename>')
def static_files(filename):
    # 只返回常见的静态资源，避免拦截 API
    if filename.endswith(('.css', '.js', '.json', '.ico', '.png', '.jpg', '.jpeg', '.gif', '.svg')):
        return send_from_directory(resource_path('.'), filename)
    return jsonify({'error': 'Not found'}), 404

# ---------- Flask 运行函数 ----------
def run_flask(port):
    print(f"[INFO] 图片文件夹路径: {UPLOAD_FOLDER}")
    print(f"[INFO] 数据文件路径: {PROMPT_FILE}")
    print(f"[INFO] Flask 正在运行于 http://127.0.0.1:{port}")
    # threaded=True：避免某个慢请求（如更新检查）堵住整个页面资源加载
    app.run(debug=False, host='127.0.0.1', port=port, threaded=True)

# ====================================================================
# 单实例锁定：防止同时打开两个窗口
# ====================================================================
_INSTANCE_LOCK_DIR = os.path.join(tempfile.gettempdir(), 'NovelAIPromptTool')
_INSTANCE_LOCK_FILE = os.path.join(_INSTANCE_LOCK_DIR, 'instance.lock')
_instance_fd = None


def _show_duplicate_instance_dialog():
    """弹出 Windows 原生提示框，告知程序已在运行"""
    try:
        ctypes.windll.user32.MessageBoxW(
            0,
            '程序已在运行中，请勿重复启动。',
            'NovelAI 提示词记录',
            0x40 | 0x00000000  # MB_ICONINFORMATION | MB_OK
        )
    except Exception:
        pass


def _acquire_instance_lock():
    """尝试获取单实例锁。成功返回 True，失败返回 False。"""
    global _instance_fd
    os.makedirs(_INSTANCE_LOCK_DIR, exist_ok=True)
    try:
        _instance_fd = os.open(_INSTANCE_LOCK_FILE, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        os.write(_instance_fd, str(os.getpid()).encode())
        return True
    except OSError:
        return False


def _release_instance_lock():
    """释放单实例锁，清理锁文件。"""
    global _instance_fd
    try:
        if _instance_fd is not None:
            try:
                os.close(_instance_fd)
            except OSError:
                pass
            _instance_fd = None
    finally:
        try:
            os.remove(_INSTANCE_LOCK_FILE)
        except OSError:
            pass


def _check_existing_instance():
    """检查锁文件对应的进程是否还活着（进程被强制 kill 后锁文件会残留）"""
    if not os.path.exists(_INSTANCE_LOCK_FILE):
        return False
    try:
        with open(_INSTANCE_LOCK_FILE, 'r') as f:
            pid = int(f.read().strip())
        os.kill(pid, 0)
        return True
    except (ProcessLookupError, ValueError, OSError):
        return False


# ---------- 入口 ----------
if __name__ == '__main__':
    setup_logging()

    # ----- 单实例检查 -----
    if not _acquire_instance_lock():
        if _check_existing_instance():
            _show_duplicate_instance_dialog()
            sys.exit(0)
        else:
            _release_instance_lock()
            if not _acquire_instance_lock():
                _show_duplicate_instance_dialog()
                sys.exit(0)
    atexit.register(_release_instance_lock)

    # prompt.json 启动检查：备份 / 比对 / 语法错误提取
    recovery_status, recovery_params = ensure_prompt_backup()
    if recovery_status == 'recovery':
        print('[INFO] 检测到 prompt.json 语法错误，等待前端用户确认恢复')

    available_port = find_available_port(5000)
    print(f"[INFO] 使用端口: {available_port}")

    flask_thread = threading.Thread(target=run_flask, args=(available_port,), daemon=True)
    flask_thread.start()

    time.sleep(0.1)  # 等待 Flask 启动

    # prompt.json 运行时监控（防人为修改）
    start_prompt_monitor()

    window = webview.create_window(
        'NovelAI 提示词记录',
        f'http://127.0.0.1:{available_port}',
        width=1100,
        height=800,
        resizable=True
    )
    # 保持默认隐私模式：避免浏览器把输入框内容记住并在打包时弹出历史建议。
    # 更新提示改为由后端缓存文件维护，不再依赖 localStorage。
    webview.start()

    sys.exit(0)