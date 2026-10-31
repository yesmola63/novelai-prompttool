export const state = {
    allData: {},          // 所有提示词数据
    flatItems: [],        // 展平的提示词列表（用于索引）
    currentModalIndex: -1,
    currentImageIndex: 0,
    currentImages: [],
    currentCategory: '全部',
    navPage: 0,           // 分类导航分页页码（0 起始）
    loadAbortController: null,
    loadingImage: null,

    // 编辑状态
    editing: {
        originalGroup: '',
        originalRole: '',
        originalCommon: '',
        existingImages: [],
        pendingFiles: []
    },

    // 添加状态
    addPendingFiles: [],

    // prompt.json 恢复模式：非空表示正在恢复损坏数据 {group: [items]}
    recoveryPending: null
};