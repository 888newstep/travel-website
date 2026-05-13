<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import {
  Search,
  MapPin,
  Calendar,
  Users,
  ChevronRight,
  Star,
  Compass,
  Plane,
  Hotel,
  Camera,
  Menu,
  X,
  ArrowRight,
  ArrowLeft,
  Check,
  Clock,
  User,
  Settings,
  Heart,
  History,
  LogOut,
  Bell,
  Map,
  CreditCard,
  Briefcase,
  Navigation,
  MessageSquare,
  Share2,
  Wallet,
  Bus,
  ShieldAlert,
  Sparkles,
  Utensils,
  BookOpen,
  ThumbsUp,
  Download,
  BarChart3,
  PieChart,
  TrendingUp,
  Users2,
  UserPlus,
  Lock,
  Unlock,
  FileText,
  Activity,
  Eye,
  EyeOff,
  Copy,
  Trash2,
  Edit3,
  CheckCircle2,
  AlertCircle,
  FileUp,
  FileDown,
  FolderPlus,
  Mic,
  Volume2,
  Languages,
  Package,
  Sun,
  CloudRain,
  Thermometer,
  Smile,
  Frown,
  Meh,
  CircleDollarSign,
  Info,
  Lightbulb
} from 'lucide-vue-next';
import { Motion, Presence } from '@motionone/vue';
import { userApi } from './api/user.api';
import { collectionApi } from './api/collection.api';
import { noteApi } from './api/note.api';
import { shareApi } from './api/share.api';

// ... existing code ...



const isMenuOpen = ref(false);
const showNotifications = ref(false);
const showPlacePreview = ref(false);
const placeSearchQuery = ref('');
const currentView = ref<'home' | 'profile' | 'detail' | 'itineraries' | 'community' | 'planning' | 'resources'>('home');
const previousView = ref<'home' | 'profile' | 'detail' | 'itineraries' | 'community' | 'planning' | 'resources'>('home');
const planningMode = ref<'personalized' | 'popular' | 'seasonal' | 'theme' | 'guide' | 'budget' | 'smart'>('personalized');
const selectedTheme = ref('文化');
const selectedSeason = ref('春季');
const showCompareModal = ref(false);
const routesToCompare = ref<any[]>([]);
const showAdjustmentModal = ref(false);
const activeRouteForAdjustment = ref<any>(null);
const selectedItineraryForAdjustment = ref<any>(null);
const selectedRecommendation = ref<any>(null);
const searchQuery = ref('');
const selectedDate = ref('');
const tripDuration = ref(7);
const guestCount = ref(1);
const userStyles = ref<string[]>([]);
const newStyle = ref('');
const budgetRange = ref(2000);
const transportMode = ref('mixed');
const showAIAssistant = ref(false);
const aiMessage = ref('');
const aiMode = ref<'chat' | 'translate' | 'budget' | 'safety' | 'diary' | 'packing' | 'bestTime'>('chat');
const chatHistory = ref<{role: 'user' | 'ai', text: string}[]>([]);
const isAnalyzingImage = ref(false);
const analyzedImageResult = ref<any>(null);

// User & Auth State (based on UserController)
interface UserProfile {
  id: number;
  username: string;
  phone: string;
  avatar: string;
  role: 'admin' | 'user';
  stats: {
    notes: number;
    collections: number;
    shares: number;
  }
}

interface ApiUser {
  id: number;
  username: string;
  phone: string;
  avatar: string;
  role: 'admin' | 'user';
  stats?: {
    notes: number;
    collections: number;
    shares: number;
  };
}
// ... existing code ...
const currentUser = ref<UserProfile | null>(null);
const showAuthModal = ref(false);
const authMode = ref<'login' | 'register'>('login');
const loginForm = ref({ username: '', password: '' });
const registerForm = ref({ username: '', phone: '', password: '', captcha: '', agreement: false });
const showCaptchaButton = ref(false);
const captchaTimer = ref(0);
const showChangePasswordModal = ref(false);
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' });
const notifications = ref<any[]>([]);
const collectionCategories = ref<string[]>([]);
const selectedCollectionCategory = ref<string | null>(null);
const showValidateShareModal = ref(false);
const validateShareCode = ref('');
const shareInfo = ref<any>(null);
// ... existing code ...

// 发送验证码
const handleSendCaptcha = async () => {
  if (!registerForm.value.phone) {
    alert('请输入手机号');
    return;
  }

  try {
    await userApi.sendCaptcha(registerForm.value.phone);
    alert('验证码已发送');
    captchaTimer.value = 60;
    const timer = setInterval(() => {
      captchaTimer.value--;
      if (captchaTimer.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    console.error('发送验证码失败:', error);
    alert('发送验证码失败，请重试');
  }
};

// 注册
const handleRegister = async () => {
  if (!registerForm.value.agreement) {
    alert('请阅读并同意用户协议');
    return;
  }

  try {
    await userApi.register({
      username: registerForm.value.username,
      phone: registerForm.value.phone,
      password: registerForm.value.password,
      captcha: registerForm.value.captcha,
      agreement: registerForm.value.agreement
    });

    alert('注册成功，请登录');
    authMode.value = 'login';
    registerForm.value = { username: '', phone: '', password: '', captcha: '', agreement: false };
  } catch (error) {
    console.error('注册失败:', error);
    alert('注册失败，请检查输入信息');
  }
};

// 修改密码
const handleChangePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('两次输入的密码不一致');
    return;
  }

  try {
    await userApi.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    });
    alert('密码修改成功');
    showChangePasswordModal.value = false;
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
  } catch (error) {
    console.error('修改密码失败:', error);
    alert('修改密码失败，请检查原密码');
  }
};

// 加载通知
const loadNotifications = async () => {
  try {
    const result: any = await userApi.getNotifications(0, 20);
    notifications.value = result || [];
  } catch (error) {
    console.error('获取通知失败:', error);
  }
};

// 加载收藏列表
const loadUserCollections = async () => {
  if (!currentUser.value) return;

  try {
    const collections: any = await collectionApi.getUserCollections(currentUser.value.id, 0, 50);
    userCollections.value = collections || [];

    const categories: any = await collectionApi.getCollectionCategories(currentUser.value.id);
    collectionCategories.value = categories || [];
  } catch (error) {
    console.error('获取收藏列表失败:', error);
  }
};

// 按分类加载收藏
const loadCollectionsByCategory = async (category: string) => {
  if (!currentUser.value) return;

  try {
    const collections: any = await collectionApi.getCollectionsByCategory(
        currentUser.value.id,
        category,
        0,
        50
    );
    userCollections.value = collections || [];
  } catch (error) {
    console.error('获取分类收藏失败:', error);
  }
};

// 验证分享码
const handleValidateShareCode = async () => {
  if (!validateShareCode.value) {
    alert('请输入分享码');
    return;
  }

  try {
    const isValid: any = await shareApi.validateShareCode(validateShareCode.value);
    if (isValid) {
      const info: any = await shareApi.getShareInfo(validateShareCode.value);
      shareInfo.value = info;
      alert('分享码有效');
    } else {
      alert('分享码无效或已过期');
    }
  } catch (error) {
    console.error('验证分享码失败:', error);
    alert('验证分享码失败');
  }
};

const handleLogin = async () => {
  try {
    const loginResponse: any = await userApi.login({
      username: loginForm.value.username,
      password: loginForm.value.password,
    });

    const token = loginResponse.token || loginResponse;
    localStorage.setItem('token', token);

    const userInfo: any = await userApi.getCurrentUser();
    currentUser.value = {
      id: userInfo.id || 1,
      username: userInfo.username || loginForm.value.username,
      phone: userInfo.phone || '138****0000',
      avatar: userInfo.avatar || 'https://picsum.photos/seed/user/100/100',
      role: (userInfo.role || 'user') as 'admin' | 'user',
      stats: userInfo.stats || { notes: 12, collections: 5, shares: 8 }
    };
    showAuthModal.value = false;
  } catch (error) {
    console.error('登录失败:', error);
    alert('登录失败，请检查用户名和密码');
  }
};



const handleLogout = async () => {
  try {
    await userApi.logout();
  } catch (error) {
    console.error('登出请求失败:', error);
  } finally {
    localStorage.removeItem('token');
    currentUser.value = null;
    currentView.value = 'home';
  }
};



// Route Collections State (based on RouteCollectionController)
const userCollections = ref<any[]>([]);

// Route Sharing State (based on RouteShareController and FileShareController)
const showShareModal = ref(false);
const activeItemForSharing = ref<any>(null);
const shareCodeResponse = ref<string | null>(null);
const isGeneratingShareCode = ref(false);

const generateShareCode = async (item: any) => {
  isGeneratingShareCode.value = true;
  activeItemForSharing.value = item;
  showShareModal.value = true;

  try {
    const response: any = await shareApi.generateShareCode(item.id, 'itinerary');
    const code = response.code || response;
    shareCodeResponse.value = code;
  } catch (error) {
    console.error('生成分享码失败:', error);
    alert('生成分享码失败，请重试');
  }
};
  
  // Simulate API call to RouteShareController/FileShareController
  setTimeout(() => {
    shareCodeResponse.value = Math.random().toString(36).substring(2, 8).toUpperCase();
    isGeneratingShareCode.value = false;
  }, 1000);

// Collaboration State (based on TripCollaborationController)
interface Collaborator {
  id: number;
  name: string;
  avatar: string;
  role: 'owner' | 'editor' | 'viewer';
  isOnline: boolean;
}

interface TripTask {
  id: number;
  description: string;
  assigneeId: number;
  isCompleted: boolean;
}

// Optimization State (based on RouteOptimizationController)
interface OptimizationSuggestion {
  id: number;
  type: 'time' | 'cost' | 'distance';
  title: string;
  description: string;
  impact: string;
}

// Analytics State (based on TravelAnalyticsController)
interface PlatformOverview {
  totalUsers: number;
  activeRoutes: number;
  completedTrips: number;
  avgSatisfaction: number;
}

const showCollaborationModal = ref(false);
const activeTripForCollaboration = ref<any>(null);
const collaborators = ref<Collaborator[]>([
  { id: 1, name: '陈雪', avatar: 'https://picsum.photos/seed/user1/100/100', role: 'owner', isOnline: true },
  { id: 2, name: '小明', avatar: 'https://picsum.photos/seed/user2/100/100', role: 'editor', isOnline: false },
  { id: 3, name: '旅游达人', avatar: 'https://picsum.photos/seed/user3/100/100', role: 'viewer', isOnline: true }
]);

const tripTasks = ref<TripTask[]>([
  { id: 1, description: '预订京都酒店', assigneeId: 1, isCompleted: true },
  { id: 2, description: '确认岚山小火车票', assigneeId: 2, isCompleted: false },
  { id: 3, description: '准备签证材料', assigneeId: 1, isCompleted: false }
]);

const optimizationSuggestions = ref<OptimizationSuggestion[]>([
  { id: 1, type: 'time', title: '时间优化', description: '建议将岚山行程移至周二，可避开 30% 的人流。', impact: '节省 1.5 小时' },
  { id: 2, type: 'cost', title: '成本优化', description: '购买关西周游卡可节省约 2000 日元交通费。', impact: '节省 ¥120' },
  { id: 3, type: 'distance', title: '距离优化', description: '调整金阁寺与龙安寺的访问顺序，减少 5km 步行。', impact: '减少 5km' }
]);

const platformOverview = ref<PlatformOverview>({
  totalUsers: 12540,
  activeRoutes: 842,
  completedTrips: 3210,
  avgSatisfaction: 4.8
});

const showAnalyticsModal = ref(false);
const showOptimizationHistory = ref(false);
const optimizationHistory = ref([
  { id: 1, date: '2024-04-10', type: '时间', result: '减少等待时间 45 分钟' },
  { id: 2, date: '2024-04-12', type: '路线', result: '优化了景点访问顺序' }
]);

// Resource Management State (based on ResourceFileController, FileCategoryController, FileVersionController)
interface ResourceFile {
  id: number;
  fileName: string;
  category: string;
  description: string;
  size: string;
  uploadTime: string;
  version: string;
  url: string;
  tags: string[];
}

interface FileTag {
  id: number;
  tagName: string;
  parentId: number | null;
}

const resourceFiles = ref<ResourceFile[]>([
  { id: 1, fileName: '京都签证申请表.pdf', category: '文档', description: '2024年10月旅行签证', size: '1.2MB', uploadTime: '2024-03-15', version: 'v1.0', url: '#', tags: ['签证', '京都'] },
  { id: 2, fileName: '行程确认单.docx', category: '文档', description: '最终行程确认', size: '450KB', uploadTime: '2024-03-20', version: 'v2.1', url: '#', tags: ['行程', '确认'] },
  { id: 3, fileName: '岚山风景照.jpg', category: '图片', description: '去年秋天的参考图', size: '2.4MB', uploadTime: '2023-11-10', version: 'v1.0', url: 'https://images.unsplash.com/photo-1545569341-9eb8b30979d9?q=80&w=2070&auto=format&fit=crop', tags: ['风景', '参考'] }
]);

const fileCategories = ref<FileTag[]>([
  { id: 1, tagName: '文档', parentId: null },
  { id: 2, tagName: '图片', parentId: null },
  { id: 3, tagName: '签证', parentId: 1 },
  { id: 4, tagName: '行程', parentId: 1 }
]);

const selectedCategory = ref<string | null>(null);
const fileSearchQuery = ref('');
const showFileUploadModal = ref(false);
const showVersionHistoryModal = ref(false);
const activeFileForVersions = ref<ResourceFile | null>(null);

// Smart Itinerary State (based on AISmartItineraryController)
const smartItineraryPreferences = ref({
  interests: ['文化', '美食'],
  pace: 'moderate',
  accommodationStyle: 'boutique'
});

const satisfactionPrediction = ref<number | null>(null);
const alternativeItineraries = ref<any[]>([]);

// Audio Guide State (based on AIAssistantController)
const isAudioGuidePlaying = ref(false);
const currentAudioGuideText = ref('');
const showPhotoTips = ref(false);
const photoTips = ref<string[]>([]);
const attractionIntro = ref('');

// Real-time Status State (based on RealtimeStatusController)
interface AttractionRealtimeStatus {
  attractionId: number;
  crowdCount: number;
  capacity: number;
  weather: string;
  temperature: number;
  status: 'open' | 'closed' | 'busy';
  lastUpdated: string;
  historicalAvg: number;
  sevenDayAvg: number;
}

const ATTRACTION_REALTIME_DATA = ref<Record<number, AttractionRealtimeStatus>>({
  1: { 
    attractionId: 1, crowdCount: 450, capacity: 1000, weather: '晴朗', temperature: 22, 
    status: 'open', lastUpdated: new Date().toISOString(), historicalAvg: 380, sevenDayAvg: 410 
  },
  2: { 
    attractionId: 2, crowdCount: 850, capacity: 1200, weather: '多云', temperature: 18, 
    status: 'busy', lastUpdated: new Date().toISOString(), historicalAvg: 600, sevenDayAvg: 750 
  },
  3: { 
    attractionId: 3, crowdCount: 120, capacity: 500, weather: '小雨', temperature: 15, 
    status: 'open', lastUpdated: new Date().toISOString(), historicalAvg: 150, sevenDayAvg: 130 
  }
});

const getRealtimeStatus = (id: number) => {
  return ATTRACTION_REALTIME_DATA.value[id] || {
    attractionId: id, crowdCount: 0, capacity: 1000, weather: '未知', temperature: 0,
    status: 'open', lastUpdated: new Date().toISOString(), historicalAvg: 0, sevenDayAvg: 0
  };
};

const SYSTEM_CONFIG = {
  appName: "智慧旅游系统",
  version: "1.0.0",
  status: "running",
  features: ["AI智能推荐", "路线规划", "实时数据", "用户社区"]
};

const SYSTEM_STATS = ref({
  apiCalls: { today: 12345, thisWeek: 87654 },
  userActivity: { activeUsers: 5678, totalUsers: 45678 },
  performance: { responseTime: "23ms", uptime: "99.9%" },
  health: { status: "healthy", database: "connected", aiService: "available" }
});

const TRAVEL_STYLES = [
  "冒险", "文化", "放松", "奢华", "经济", "自然", "美食", "摄影"
];

const addStyle = (style: string) => {
  if (!userStyles.value.includes(style)) {
    userStyles.value.push(style);
  } else {
    userStyles.value = userStyles.value.filter(s => s !== style);
  }
};

const addNewCustomStyle = () => {
  if (newStyle.value.trim() && !userStyles.value.includes(newStyle.value.trim())) {
    userStyles.value.push(newStyle.value.trim());
    newStyle.value = '';
  }
};

const RECOMMENDATIONS = [
  {
    id: 101,
    title: "奈良秘境寺庙",
    location: "日本，奈良",
    image: "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?q=80&w=2070&auto=format&fit=crop",
    tags: ["文化", "宁静"],
    description: "探索奈良古寺的宁静之美，避开喧嚣的游客路径。体验日本第一个永久首都的精神宁静和建筑奇迹。",
    highlights: ["古建筑", "鹿公园", "传统茶馆"]
  },
  {
    id: 102,
    title: "阿马尔菲海岸自驾",
    location: "意大利，阿马尔菲",
    image: "https://images.unsplash.com/photo-1533105079780-92b9be482077?q=80&w=2070&auto=format&fit=crop",
    tags: ["浪漫", "美景"],
    description: "踏上世界上最令人惊叹的海岸公路之一。阿马尔菲海岸提供了悬挂在土耳其蓝地中海上方陡峭悬崖上的彩色村庄的绝佳景观。",
    highlights: ["海岸景观", "意大利美食", "奢华度假村"]
  },
  {
    id: 103,
    title: "塞伦盖蒂野生动物追踪",
    location: "坦桑尼亚",
    image: "https://images.unsplash.com/photo-1516426122078-c23e76319801?q=80&w=2070&auto=format&fit=crop",
    tags: ["冒险", "野生动物"],
    description: "在非洲心脏地带见证大自然的原始力量。塞伦盖蒂是大迁徙和令人难以置信的野生动物多样性的家园，提供难忘的游猎体验。",
    highlights: ["非洲五霸", "热气球", "生态旅馆"]
  }
];

const DESTINATIONS = [
  {
    id: 1,
    name: "日本，京都",
    image: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=2070&auto=format&fit=crop",
    rating: 4.9,
    price: "¥8,500",
    category: "文化"
  },
  {
    id: 2,
    name: "希腊，圣托里尼",
    image: "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?q=80&w=2070&auto=format&fit=crop",
    rating: 4.8,
    price: "¥10,500",
    category: "浪漫"
  },
  {
    id: 3,
    name: "瑞士，阿尔卑斯山",
    image: "https://images.unsplash.com/photo-1531310197839-ccf54634509e?q=80&w=2070&auto=format&fit=crop",
    rating: 4.9,
    price: "¥15,000",
    category: "冒险"
  },
  {
    id: 4,
    name: "印度尼西亚，巴厘岛",
    image: "https://images.unsplash.com/photo-1537996194471-e657df975ab4?q=80&w=2070&auto=format&fit=crop",
    rating: 4.7,
    price: "¥5,600",
    category: "热带"
  }
];

const PLANNED_ITINERARIES = ref([
  {
    id: 1,
    title: "京都之秋",
    destination: "日本，京都",
    dates: "2024年10月15日 - 10月22日",
    image: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=2070&auto=format&fit=crop",
    status: "已确认",
    days: 7,
    activities: 12,
    isPublic: true,
    collaborators: 3,
    completionRate: 85,
    isCollected: false
  },
  {
    id: 2,
    title: "希腊跳岛游",
    destination: "圣托里尼 & 米科诺斯",
    dates: "2024年6月5日 - 6月15日",
    image: "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?q=80&w=2072&auto=format&fit=crop",
    status: "规划中",
    days: 10,
    activities: 8,
    isPublic: false,
    collaborators: 1,
    completionRate: 45,
    isCollected: false
  },
  {
    id: 3,
    title: "阿尔卑斯冒险",
    destination: "瑞士阿尔卑斯山",
    dates: "2024年12月20日 - 12月27日",
    image: "https://images.unsplash.com/photo-1531310197839-ccf54634509e?q=80&w=2070&auto=format&fit=crop",
    status: "草稿",
    days: 8,
    activities: 5,
    isPublic: false,
    collaborators: 2,
    completionRate: 20,
    isCollected: false
  }
]);

const MOCK_SPOTS = {
  '京都': [
    { name: '伏见稻荷大社', image: 'https://images.unsplash.com/photo-1528164344705-47542687990d?q=80&w=2070&auto=format&fit=crop' },
    { name: '岚山竹林', image: 'https://images.unsplash.com/photo-1545569341-9eb8b30979d9?q=80&w=2070&auto=format&fit=crop' },
    { name: '金阁寺', image: 'https://images.unsplash.com/photo-1505069194752-51c76279c324?q=80&w=2070&auto=format&fit=crop' }
  ],
  '圣托里尼': [
    { name: '伊亚小镇', image: 'https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?q=80&w=2070&auto=format&fit=crop' },
    { name: '红海滩', image: 'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?q=80&w=2070&auto=format&fit=crop' },
    { name: '阿克罗蒂里灯塔', image: 'https://images.unsplash.com/photo-1533105079780-92b9be482077?q=80&w=2070&auto=format&fit=crop' }
  ],
  '巴厘岛': [
    { name: '乌鲁瓦图寺', image: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?q=80&w=2070&auto=format&fit=crop' },
    { name: '德格拉朗梯田', image: 'https://images.unsplash.com/photo-1559628233-100c798642d4?q=80&w=2070&auto=format&fit=crop' },
    { name: '圣猴森林公园', image: 'https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?q=80&w=2070&auto=format&fit=crop' }
  ]
};

const filteredSpots = computed(() => {
  if (!placeSearchQuery.value) return [];
  const query = placeSearchQuery.value.toLowerCase();
  const key = Object.keys(MOCK_SPOTS).find(k => k.toLowerCase().includes(query)) as keyof typeof MOCK_SPOTS | undefined;
  return key ? MOCK_SPOTS[key] : [];
});

const travelNotes = ref([
  {
    id: 1,
    title: "京都心之所向的一周",
    author: "陈雪",
    image: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=2070&auto=format&fit=crop",
    likes: 1240,
    comments: 86,
    isLiked: false,
    isCollected: false,
    excerpt: "秋天的京都每个人都应该至少体验一次。色彩鲜艳，寺庙宁静...",
    commentList: [
      { id: 1, user: "小明", text: "京都的秋天真的太美了！" },
      { id: 2, user: "旅游达人", text: "这篇攻略很详细，收藏了。" }
    ]
  },
  {
    id: 2,
    title: "背包穿越瑞士阿尔卑斯山",
    author: "马克·韦伯",
    image: "https://images.unsplash.com/photo-1531310197839-ccf54634509e?q=80&w=2070&auto=format&fit=crop",
    likes: 850,
    comments: 42,
    isLiked: false,
    isCollected: false,
    excerpt: "空气稀薄，但景色美不胜收。这是我为经济型旅行者准备的最佳路线指南...",
    commentList: [
      { id: 1, user: "登山爱好者", text: "阿尔卑斯山是我的梦想之地。" }
    ]
  }
]);

const likeNote = async (note: any) => {
  if (!currentUser.value) {
    alert('请先登录');
    return;
  }

  try {
    if (!note.isLiked) {
      await noteApi.likeNote(note.id, currentUser.value!.id);
      note.isLiked = true;
      note.likes += 1;
    } else {
      await noteApi.unlikeNote(note.id, currentUser.value!.id);
      note.isLiked = false;
      note.likes -= 1;
    }
  } catch (error) {
    console.error('点赞操作失败:', error);
    alert('操作失败，请重试');
  }
};
const collectNote = async (note: any) => {
  if (!currentUser.value) {
    alert('请先登录');
    return;
  }

  try {
    if (!note.isCollected) {
      await noteApi.collectNote(note.id);
      note.isCollected = true;
    } else {
      await noteApi.uncollectNote(note.id);
      note.isCollected = false;
    }
  } catch (error) {
    console.error('收藏操作失败:', error);
    alert('操作失败，请重试');
  }
};

const copyRoute = (itinerary: any) => {
  const newItinerary = {
    ...itinerary,
    id: Date.now(),
    title: `${itinerary.title} (副本)`,
    status: '草稿',
    completionRate: 0
  };
  PLANNED_ITINERARIES.value.push(newItinerary);
};

const deleteRoute = (id: number) => {
  PLANNED_ITINERARIES.value = PLANNED_ITINERARIES.value.filter(i => i.id !== id);
};

const toggleVisibility = (itinerary: any) => {
  itinerary.isPublic = !itinerary.isPublic;
};

const openCollaboration = (itinerary: any) => {
  activeTripForCollaboration.value = itinerary;
  showCollaborationModal.value = true;
};

const openAnalytics = () => {
  showAnalyticsModal.value = true;
};

const toggleTask = (task: TripTask) => {
  task.isCompleted = !task.isCompleted;
};

const applyOptimization = (suggestion: OptimizationSuggestion) => {
  // Simulate applying optimization
  console.log(`已应用优化: ${suggestion.title}`);
};

const showCommentsModal = ref(false);
const selectedNoteForComments = ref<any>(null);
const newCommentText = ref('');
const showPostNoteModal = ref(false);
const newNoteData = ref({
  title: '',
  excerpt: '',
  image: ''
});

const openComments = (note: any) => {
  selectedNoteForComments.value = note;
  showCommentsModal.value = true;
};

const addComment = () => {
  if (!newCommentText.value.trim() || !selectedNoteForComments.value) return;
  
  const newComment = {
    id: Date.now(),
    user: '我',
    text: newCommentText.value
  };
  
  selectedNoteForComments.value.commentList.push(newComment);
  selectedNoteForComments.value.comments++;
  newCommentText.value = '';
};

const postNote = async () => {
  if (!newNoteData.value.title || !newNoteData.value.excerpt) return;

  try {
    const response: any = await noteApi.createNote({
      travelNote: {
        title: newNoteData.value.title,
        author: currentUser.value?.username || '我',
        image: newNoteData.value.image || `https://picsum.photos/seed/${Date.now()}/800/600`,
        excerpt: newNoteData.value.excerpt,
        isLiked: false,
        isCollected: false,
        commentList: []
      },
      tags: []
    });


    const newNote = response.data || response;
    travelNotes.value.unshift(newNote);
    showPostNoteModal.value = false;
    newNoteData.value = { title: '', excerpt: '', image: '' };
    alert('发布成功');
  } catch (error) {
    console.error('发布笔记失败:', error);
    alert('发布失败，请重试');
  }
};


const deleteNote = async (noteId: number) => {
  if (!confirm('确定要删除这条笔记吗？')) return;

  try {
    await noteApi.deleteNote(noteId, currentUser.value!.id);
    travelNotes.value = travelNotes.value.filter(n => n.id !== noteId);
    alert('删除成功');
  } catch (error) {
    console.error('删除笔记失败:', error);
    alert('删除失败，请重试');
  }
};

// Intelligent Route Planning Logic
const toggleCompare = (route: any) => {
  const index = routesToCompare.value.findIndex(r => r.id === route.id);
  if (index > -1) {
    routesToCompare.value.splice(index, 1);
  } else {
    if (routesToCompare.value.length >= 3) {
      alert('最多只能比较 3 条路线');
      return;
    }
    routesToCompare.value.push(route);
  }
};

const getAdjustment = (itinerary: any) => {
  selectedItineraryForAdjustment.value = itinerary;
  showAdjustmentModal.value = true;
};

const startPlanning = () => {
  currentView.value = 'planning';
};

const REALTIME_ALERTS = ref([
  { id: 1, type: 'weather', title: '京都大雨预警', description: '预计下午3:00开始。建议进行室内活动。', severity: 'medium', category: 'weather' },
  { id: 2, type: 'crowd', title: '伏见稻荷大社人流激增', description: '当前拥挤度 92%，建议错峰游览。', severity: 'high', category: 'crowd' },
  { id: 3, type: 'status', title: '系统同步完成', description: '所有景点实时数据已更新。', severity: 'low', category: 'system' },
  { id: 4, type: 'traffic', title: '岚山方向交通拥堵', description: '由于道路施工，预计延迟20分钟。', severity: 'medium', category: 'traffic' }
]);

const syncAllData = () => {
  SYSTEM_STATS.value.health.status = 'syncing';
  setTimeout(() => {
    SYSTEM_STATS.value.health.status = 'healthy';
    ATTRACTION_REALTIME_DATA.value[1].lastUpdated = new Date().toISOString();
    ATTRACTION_REALTIME_DATA.value[2].lastUpdated = new Date().toISOString();
    ATTRACTION_REALTIME_DATA.value[3].lastUpdated = new Date().toISOString();
    REALTIME_ALERTS.value.unshift({
      id: Date.now(),
      type: 'status',
      title: '手动同步成功',
      description: '已成功同步 3 个景点的最新状态。',
      severity: 'low',
      category: 'system'
    });
  }, 1500);
};

const sendAIMessage = () => {
  if (!aiMessage.value.trim()) return;
  chatHistory.value.push({ role: 'user', text: aiMessage.value });
  const userMsg = aiMessage.value;
  const currentMode = aiMode.value;
  aiMessage.value = '';
  
  setTimeout(() => {
    let responseText = '';
    if (currentMode === 'translate') {
      responseText = `[翻译结果] “${userMsg}” 翻译为英文是： "Where is the nearest authentic restaurant?"`;
    } else if (currentMode === 'budget') {
      responseText = `[预算估算] 根据您的需求，在 ${userMsg} 旅行 5 天的预估费用约为 ¥4,500 - ¥6,000，包含住宿、餐饮和交通。`;
    } else if (currentMode === 'safety') {
      responseText = `[安全建议] 关于 ${userMsg} 的安全提示：当地治安良好，但建议在人流密集区注意财物安全。紧急电话：110/119。`;
    } else {
      responseText = `我已经分析了您关于“${userMsg}”的请求。根据您的偏好，我建议您查看我们为您精选的推荐地点，并为您的下一次旅行使用智能路线生成器！`;
    }
    
    chatHistory.value.push({ 
      role: 'ai', 
      text: responseText 
    });
  }, 1000);
};

const analyzeImage = () => {
  isAnalyzingImage.value = true;
  setTimeout(() => {
    isAnalyzingImage.value = false;
    analyzedImageResult.value = {
      attraction: "清水寺 (Kiyomizu-dera)",
      confidence: 0.98,
      tags: ["历史建筑", "世界文化遗产", "京都"],
      advice: "建议在清晨访问以避开人群，并体验著名的清水舞台。"
    };
    chatHistory.value.push({
      role: 'ai',
      text: `[图像识别成功] 识别到景点：${analyzedImageResult.value.attraction}。${analyzedImageResult.value.advice}`
    });
  }, 2000);
};

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

const toggleCollection = async (item: any) => {
  if (!currentUser.value) {
    alert('请先登录');
    return;
  }

  try {
    if (!item.isCollected) {
      await collectionApi.addCollection({
        userId: currentUser.value.id,
        routeId: item.id,
      });
      item.isCollected = true;
      userCollections.value.push(item);
    } else {
      await collectionApi.removeCollection(currentUser.value.id, item.id);
      item.isCollected = false;
      userCollections.value = userCollections.value.filter(i => i.id !== item.id);
    }
  } catch (error) {
    console.error('收藏操作失败:', error);
    alert('收藏操作失败，请重试');
  }
};


const handlePlaceBlur = () => {
  setTimeout(() => {
    showPlacePreview.value = false;
  }, 200);
};

// Hot Notes Computed (based on TravelNoteController)
const hotNotes = computed(() => {
  return [...travelNotes.value].sort((a, b) => b.likes - a.likes).slice(0, 3);
});

// Reply logic for comments (based on RouteCommentController)
const replyToId = ref<number | null>(null);
const replyUsername = ref('');

const setReply = (commentId: number, username: string) => {
  replyToId.value = commentId;
  replyUsername.value = username;
  newCommentText.value = `@${username} `;
};

const submitComment = () => {
  if (!newCommentText.value.trim() || !selectedNoteForComments.value) return;
  
  const comment = {
    id: Date.now(),
    user: currentUser.value ? currentUser.value.username : '游客',
    text: newCommentText.value,
    likes: 0,
    replies: []
  };
  
  if (replyToId.value) {
    // Add as reply
    const parent = selectedNoteForComments.value.commentList.find((c: any) => c.id === replyToId.value);
    if (parent) {
      if (!parent.replies) parent.replies = [];
      parent.replies.push(comment);
    }
    replyToId.value = null;
    replyUsername.value = '';
  } else {
    selectedNoteForComments.value.commentList.push(comment);
  }
  
  selectedNoteForComments.value.comments++;
  newCommentText.value = '';
};

const viewRecommendation = (item: any) => {
  previousView.value = currentView.value;
  selectedRecommendation.value = {
    ...item,
    title: item.title || item.name,
    location: item.location || item.name || (item.author ? `由 ${item.author} 发布` : '未知地点'),
    tags: item.tags || (item.category ? [item.category] : ["旅行笔记"]),
    description: item.description || item.excerpt || `探索 ${item.name || item.title} 的魅力。这里有独特的文化体验、令人惊叹的自然景观和难忘的美食之旅。`,
    highlights: item.highlights || ["深度旅行体验", "当地人文风情", "摄影打卡圣地", "旅行心得分享"]
  };
  currentView.value = 'detail';
};

const viewItinerary = (itinerary: any) => {
  previousView.value = currentView.value;
  selectedRecommendation.value = {
    ...itinerary,
    location: itinerary.destination,
    tags: ["我的行程", itinerary.status],
    description: `这是您为“${itinerary.title}”准备的详细旅行计划。在接下来的 ${itinerary.days} 天里，您将探索 ${itinerary.destination} 的 ${itinerary.activities} 个精彩景点。我们已经为您整理好了所有的预订信息和每日路线。`,
    highlights: ["每日详细路线图", "酒店预订确认", "交通接驳指南", "当地美食推荐"]
  };
  currentView.value = 'detail';
};
const toggleInterest = (interest: string) => {
  const index = smartItineraryPreferences.value.interests.indexOf(interest);
  if (index === -1) {
    smartItineraryPreferences.value.interests.push(interest);
  } else {
    smartItineraryPreferences.value.interests.splice(index, 1);
  }
};

const generateSmartItinerary = async () => {
  // Mock generation logic
  satisfactionPrediction.value = 0;
  alternativeItineraries.value = [];

  // Simulate API call
  setTimeout(() => {
    satisfactionPrediction.value = 92;
    alternativeItineraries.value = [
      {
        title: '深度文化与美食之旅',
        description: '专为摄影与美食爱好者打造，避开人流高峰，深入当地社区。',
        duration: tripDuration.value,
        stops: 12,
        estimatedCost: 4500,
        matchScore: 95,
        tags: ['深度游', '地道美食', '摄影机位']
      },
      {
        title: '自然风光与休闲漫步',
        description: '节奏舒缓，侧重于自然公园与海滨风光，适合放松身心。',
        duration: tripDuration.value,
        stops: 8,
        estimatedCost: 3800,
        matchScore: 88,
        tags: ['自然', '悠闲', '高性价比']
      }
    ];
  }, 1500);
};

const selectItineraryPlan = (plan: any) => {
  if (!currentUser.value) {
    alert('请先登录');
    return;
  }
  const newItinerary = {
    id: Date.now(),
    title: plan.title,
    destination: '智能推荐',
    dates: '待确认',
    image: `https://picsum.photos/seed/${plan.id}/800/600`,
    status: '规划中',
    days: plan.duration,
    activities: plan.stops,
    isPublic: false,
    collaborators: 1,
    completionRate: 0,
    isCollected: false
  };
  PLANNED_ITINERARIES.value.push(newItinerary);
  alert('行程已添加到您的列表');
  currentView.value = 'itineraries';
};

const handleGeneratePersonalizedRoute = async () => {
  if (!currentUser.value) {
    alert('请先登录');
    return;
  }
  try {
    const { intelligentRouteApi } = await import('./api/route.api');
    await intelligentRouteApi.generatePersonalizedRoute({
      userPreferences: {
        interests: userStyles.value,
        budget: budgetRange.value,
        transportMode: transportMode.value
      },
      constraints: {
        avoidPeakHours: true,
        publicTransportOnly: false,
        includeLocalFood: true
      }
    });
    alert('个性化路线生成成功！');
  } catch (error) {
    console.error('生成路线失败:', error);
    alert('生成失败，请重试');
  }
};

const handleGenerateGuide = async () => {
  try {
    const cityInput = document.querySelector('input[placeholder="例如：京都"]') as HTMLInputElement;
    const daysInput = document.querySelector('input[placeholder="例如：5"]') as HTMLInputElement;
    const city = cityInput?.value || '京都';
    const days = parseInt(daysInput?.value || '5');

    const { intelligentRouteApi } = await import('./api/route.api');
    await intelligentRouteApi.getThemeRoutes('文化', 1, days);
    alert(`已为您生成 ${city} ${days}天的旅游攻略！`);
  } catch (error) {
    console.error('生成攻略失败:', error);
    alert('生成攻略失败，请重试');
  }
};

const handleEstimateBudget = async () => {
  try {
    const destInput = document.querySelector('input[placeholder="例如：巴黎"]') as HTMLInputElement;
    const destination = destInput?.value || '巴黎';

    const { aiApi } = await import('./api/ai.api');
    await aiApi.getTravelRecommendation({
      location: destination,
      budget: budgetRange.value,
      duration: tripDuration.value
    });
    alert(`预算估算完成：${destination} ${tripDuration.value}天旅行约需 ¥${(budgetRange.value * tripDuration.value).toLocaleString()}`);
  } catch (error) {
    console.error('预算估算失败:', error);
    alert('预算估算失败，请重试');
  }
};

const handleClearNotifications = () => {
  if (confirm('确定要清除所有通知吗？')) {
    notifications.value = [];
  }
};

const handleViewMap = () => {
  alert('地图功能开发中');
};

const handleAddCategory = () => {
  const name = prompt('请输入新分类名称：');
  if (name) {
    fileCategories.value.push({
      id: Date.now(),
      tagName: name,
      parentId: null
    });
  }
};

const handleShowStats = () => {
  alert('统计功能开发中');
};

const handleFileSettings = () => {
  alert('设置功能开发中');
};

const handleDownloadFile = async (file: any) => {
  try {
    alert(`开始下载：${file.fileName}`);
  } catch (error) {
    console.error('下载失败:', error);
    alert('下载失败');
  }
};

const handleDeleteFile = async (fileId: number) => {
  if (!confirm('确定要删除这个文件吗？')) return;
  resourceFiles.value = resourceFiles.value.filter(f => f.id !== fileId);
  alert('文件已删除');
};

const handleSelectDestination = (spot: any) => {
  searchQuery.value = spot.name;
  showPlacePreview.value = false;
};

const handleCopyLink = () => {
  if (shareCodeResponse.value) {
    navigator.clipboard.writeText(shareCodeResponse.value);
    alert('分享码已复制到剪贴板');
  }
};

const handleApplyOptimization = (suggestion: any) => {
  if (confirm(`确定要应用优化建议"${suggestion.title}"吗？`)) {
    applyOptimization(suggestion);
    optimizationHistory.value.push({
      id: Date.now(),
      date: new Date().toISOString().split('T')[0],
      type: suggestion.type === 'time' ? '时间' : suggestion.type === 'cost' ? '成本' : '路线',
      result: suggestion.impact
    });
    alert('优化建议已应用');
  }
};

const handleOptimizationHistory = () => {
  showOptimizationHistory.value = true;
};

onMounted(async () => {
  const token = localStorage.getItem('token');
  if (token) {
    try {
      const userInfo: any = await userApi.getCurrentUser();
      currentUser.value = {
        id: userInfo.id || 1,
        username: userInfo.username || '旅行者',
        phone: userInfo.phone || '138****0000',
        avatar: userInfo.avatar || 'https://picsum.photos/seed/user/100/100',
        role: (userInfo.role || 'user') as 'admin' | 'user',
        stats: userInfo.stats || { notes: 12, collections: 5, shares: 8 }
      };

      // 新增：加载收藏和通知
      await loadUserCollections();
      await loadNotifications();
    } catch (error) {
      console.error('获取用户信息失败:', error);
      localStorage.removeItem('token');
    }
  }

  try {
    const notes: any = await noteApi.getNotes(0, 10);
    if (notes && Array.isArray(notes)) {
      travelNotes.value = notes.map((note: any) => ({
        ...note,
        isLiked: note.isLiked || false,
        isCollected: note.isCollected || false,
        commentList: note.commentList || []
      }));
    }
  } catch (error) {
    console.error('获取笔记列表失败:', error);
  }
});

</script>

<template>
  <div class="h-screen bg-stone-50 selection:bg-emerald-100 selection:text-emerald-900 flex flex-col overflow-hidden">
    <!-- Navigation (Shrunk) -->
    <nav class="bg-white/80 backdrop-blur-md border-b border-stone-200 flex-shrink-0 relative z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-12">
          <div class="flex items-center gap-1.5">
            <div class="w-6 h-6 bg-emerald-600 rounded flex items-center justify-center">
              <Compass class="text-white w-3.5 h-3.5" />
            </div>
            <span class="text-base font-bold tracking-tight text-stone-900">智慧旅游系统</span>
          </div>

          <!-- Place Preview Search -->
          <div class="hidden lg:flex flex-1 max-w-sm mx-8 relative">
            <div class="relative w-full">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-stone-400" />
              <input 
                v-model="placeSearchQuery"
                @focus="showPlacePreview = true"
                @blur="handlePlaceBlur"
                type="text" 
                placeholder="搜索目的地（如：京都、巴厘岛）..." 
                class="w-full bg-stone-100 border-none rounded-full py-1.5 pl-9 pr-4 text-[11px] focus:ring-1 focus:ring-emerald-500 transition-all outline-none"
              />
            </div>

            <Presence>
              <Motion
                v-if="showPlacePreview && placeSearchQuery"
                :initial="{ opacity: 0, y: 10 }"
                :animate="{ opacity: 1, y: 0 }"
                :exit="{ opacity: 0, y: 10 }"
                class="absolute top-full left-0 right-0 mt-2 bg-white rounded-2xl border border-stone-200 shadow-xl z-50 p-4"
              >
                <div v-if="filteredSpots.length > 0">
                  <h4 class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mb-3">热门地点</h4>
                  <div class="space-y-2">
                    <div v-for="spot in filteredSpots" :key="spot.name" @click="handleSelectDestination(spot)" class="flex items-center gap-3 p-2 rounded-xl hover:bg-stone-50 cursor-pointer transition-colors group">
                    <div class="w-10 h-10 rounded-lg overflow-hidden flex-shrink-0">
                        <img :src="spot.image" class="w-full h-full object-cover" referrerPolicy="no-referrer" />
                      </div>
                      <div class="flex-1">
                        <p class="text-xs font-bold text-stone-900">{{ spot.name }}</p>
                        <p class="text-[9px] text-stone-500">热门地标</p>
                      </div>
                      <ArrowRight class="w-3 h-3 text-stone-300 group-hover:text-emerald-600 transition-colors" />
                    </div>
                  </div>
                </div>
                <div v-else class="py-4 text-center">
                  <p class="text-xs text-stone-400">未找到关于“{{ placeSearchQuery }}”的地点</p>
                  <p class="text-[10px] text-stone-300 mt-1">尝试搜索：京都、巴厘岛或圣托里尼</p>
                </div>
              </Motion>
            </Presence>
          </div>
          
          <div class="hidden md:flex items-center gap-6">
            <button @click="currentView = 'home'" :class="['text-xs font-medium transition-colors', currentView === 'home' ? 'text-emerald-600' : 'text-stone-600 hover:text-emerald-600']">首页</button>
            <button @click="currentView = 'itineraries'" :class="['text-xs font-medium transition-colors', currentView === 'itineraries' ? 'text-emerald-600' : 'text-stone-600 hover:text-emerald-600']">行程</button>
            <button @click="currentView = 'community'" :class="['text-xs font-medium transition-colors', currentView === 'community' ? 'text-emerald-600' : 'text-stone-600 hover:text-emerald-600']">社区</button>
            <button @click="currentView = 'resources'" :class="['text-xs font-medium transition-colors', currentView === 'resources' ? 'text-emerald-600' : 'text-stone-600 hover:text-emerald-600']">资源</button>
            
            <!-- Notifications -->
            <div class="relative">
              <button @click="showNotifications = !showNotifications" class="p-1.5 rounded-full text-stone-600 hover:bg-stone-100 transition-all relative group">
                <Bell class="w-4 h-4" />
                <span class="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
              </button>
              
              <Presence>
                <Motion
                  v-if="showNotifications"
                  :initial="{ opacity: 0, y: 10, scale: 0.95 }"
                  :animate="{ opacity: 1, y: 0, scale: 1 }"
                  :exit="{ opacity: 0, y: 10, scale: 0.95 }"
                  class="absolute right-0 mt-2 w-64 bg-white rounded-2xl border border-stone-200 shadow-xl z-50 p-4"
                >
                  <div class="flex justify-between items-center mb-3">
                    <h4 class="text-xs font-bold text-stone-900">通知</h4>
                    <button @click="handleClearNotifications" class="text-[9px] text-emerald-600 font-bold uppercase">全部清除</button>
                  </div>
                  <div class="space-y-3">
                    <div v-for="i in 2" :key="i" class="flex gap-3 p-2 rounded-xl hover:bg-stone-50 cursor-pointer transition-colors">
                      <div class="w-8 h-8 rounded-full bg-emerald-50 flex items-center justify-center flex-shrink-0">
                        <Plane class="w-4 h-4 text-emerald-600" />
                      </div>
                      <div>
                        <p class="text-[10px] font-bold text-stone-900 leading-tight">飞往京都的航班已确认！</p>
                        <p class="text-[8px] text-stone-400 mt-0.5">2小时前</p>
                      </div>
                    </div>
                  </div>
                </Motion>
              </Presence>
            </div>

            <button v-if="!currentUser" @click="showAuthModal = true" class="bg-stone-900 text-white px-5 py-1.5 rounded-full text-xs font-bold hover:bg-stone-800 transition-all">
              登录 / 注册
            </button>
            <button v-else @click="currentView = 'profile'" :class="['flex items-center gap-2 px-4 py-1.5 rounded-full transition-all border-2', currentView === 'profile' ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-white border-stone-200 text-stone-900 hover:border-emerald-600']">
              <img :src="currentUser.avatar" class="w-5 h-5 rounded-full object-cover" />
              <span class="text-xs font-bold">{{ currentUser.username }}</span>
            </button>
          </div>

          <!-- Mobile Menu Button -->
          <div class="md:hidden flex items-center">
            <button @click="toggleMenu" class="p-1.5 rounded-full text-stone-600 hover:bg-stone-100 transition-all">
              <Menu v-if="!isMenuOpen" class="w-5 h-5" />
              <X v-else class="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      <!-- Mobile Menu Overlay -->
      <Presence>
        <Motion
          v-if="isMenuOpen"
          :initial="{ opacity: 0, y: -20 }"
          :animate="{ opacity: 1, y: 0 }"
          :exit="{ opacity: 0, y: -20 }"
          class="md:hidden absolute top-full left-0 right-0 bg-white border-b border-stone-200 shadow-xl z-50 p-6 flex flex-col gap-4"
        >
          <button @click="currentView = 'home'; isMenuOpen = false" :class="['text-sm font-bold text-left', currentView === 'home' ? 'text-emerald-600' : 'text-stone-600']">首页</button>
          <button @click="currentView = 'itineraries'; isMenuOpen = false" :class="['text-sm font-bold text-left', currentView === 'itineraries' ? 'text-emerald-600' : 'text-stone-600']">行程</button>
          <button @click="currentView = 'community'; isMenuOpen = false" :class="['text-sm font-bold text-left', currentView === 'community' ? 'text-emerald-600' : 'text-stone-600']">社区</button>
          <button @click="currentView = 'resources'; isMenuOpen = false" :class="['text-sm font-bold text-left', currentView === 'resources' ? 'text-emerald-600' : 'text-stone-600']">资源</button>
          <div class="h-px bg-stone-100 my-2"></div>
          <button @click="currentView = 'profile'; isMenuOpen = false" :class="['flex items-center gap-3 text-sm font-bold', currentView === 'profile' ? 'text-emerald-600' : 'text-stone-600']">
            <User class="w-4 h-4" /> 个人资料
          </button>
        </Motion>
      </Presence>
    </nav>

    <!-- Main Content Area (Single Screen) -->
    <main class="flex-1 overflow-hidden p-3 lg:p-4">
      <!-- Route Views -->
      <div v-if="currentView === 'planning'" class="max-w-7xl mx-auto h-full flex flex-col gap-6 overflow-hidden">
        <div class="flex justify-between items-center shrink-0">
          <div>
            <h2 class="text-2xl font-serif font-bold text-stone-900">智能路线规划</h2>
            <p class="text-xs text-stone-500 mt-1">基于 AI 的多维度路线推荐与生成</p>
          </div>
          <div class="flex gap-2">
            <button 
              v-if="routesToCompare.length > 1"
              @click="showCompareModal = true"
              class="bg-amber-500 text-white px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 shadow-lg"
            >
              <ArrowRight class="w-4 h-4" /> 比较路线 ({{ routesToCompare.length }})
            </button>
            <button @click="currentView = 'home'" class="bg-white border border-stone-200 text-stone-600 px-4 py-2 rounded-xl text-xs font-bold">返回首页</button>
          </div>
        </div>

        <!-- Strategy Tabs -->
        <div class="flex gap-2 p-1 bg-stone-100 rounded-2xl self-start shrink-0">
          <button 
            v-for="mode in ['personalized', 'popular', 'seasonal', 'theme', 'guide', 'budget', 'smart']" 
            :key="mode"
            @click="planningMode = mode as any"
            :class="['px-4 py-2 rounded-xl text-xs font-bold transition-all', planningMode === mode ? 'bg-white text-emerald-600 shadow-sm' : 'text-stone-500 hover:text-stone-700']"
          >
            {{ mode === 'personalized' ? '个性化生成' : mode === 'popular' ? '热门推荐' : mode === 'seasonal' ? '季节性' : mode === 'theme' ? '主题路线' : mode === 'guide' ? '攻略生成' : mode === 'budget' ? '预算估算' : 'AI 智能规划' }}
          </button>
        </div>

        <div class="flex-1 overflow-y-auto no-scrollbar pb-10">
          <!-- Personalized Form -->
          <div v-if="planningMode === 'personalized'" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-1 space-y-6">
              <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm space-y-4">
                <h3 class="text-sm font-bold text-stone-900">生成参数</h3>
                <div class="space-y-3">
                  <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">限制条件</label>
                  <div class="space-y-2">
                    <label class="flex items-center gap-2 text-xs text-stone-600">
                      <input type="checkbox" checked class="accent-emerald-600" /> 避开人流高峰
                    </label>
                    <label class="flex items-center gap-2 text-xs text-stone-600">
                      <input type="checkbox" class="accent-emerald-600" /> 仅限公共交通
                    </label>
                    <label class="flex items-center gap-2 text-xs text-stone-600">
                      <input type="checkbox" checked class="accent-emerald-600" /> 包含当地美食
                    </label>
                  </div>
                </div>
                <button @click="handleGeneratePersonalizedRoute" class="w-full bg-emerald-600 text-white py-3 rounded-2xl text-xs font-bold shadow-lg shadow-emerald-600/20">
                  生成我的专属路线
                </button>
              </div>
            </div>
            <div class="lg:col-span-2 space-y-4">
              <div v-for="i in 2" :key="i" class="bg-white p-4 rounded-3xl border border-stone-200 shadow-sm flex gap-4 group hover:border-emerald-200 transition-all">
                <div class="w-32 h-32 rounded-2xl overflow-hidden shrink-0">
                  <img :src="`https://picsum.photos/seed/route${i}/400/400`" class="w-full h-full object-cover" />
                </div>
                <div class="flex-1 flex flex-col justify-between py-1">
                  <div>
                    <div class="flex justify-between items-start">
                      <h4 class="text-base font-bold text-stone-900">智能推荐路线 #{{ i }}</h4>
                      <button 
                        @click.stop="toggleCompare({id: i, title: `路线 #${i}`, image: `https://picsum.photos/seed/route${i}/400/400`})"
                        :class="['text-[10px] font-bold px-2 py-1 rounded-lg border transition-all', routesToCompare.some(r => r.id === i) ? 'bg-amber-50 border-amber-200 text-amber-600' : 'bg-stone-50 border-stone-200 text-stone-400']"
                      >
                        {{ routesToCompare.some(r => r.id === i) ? '已加入比较' : '+ 加入比较' }}
                      </button>
                    </div>
                    <p class="text-xs text-stone-500 mt-1">基于您的偏好：深度文化体验、美食探索</p>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex gap-2">
                      <span class="px-2 py-0.5 bg-stone-100 text-stone-500 text-[9px] font-bold rounded-full">文化</span>
                      <span class="px-2 py-0.5 bg-stone-100 text-stone-500 text-[9px] font-bold rounded-full">美食</span>
                    </div>
                    <button @click.stop="viewRecommendation({id: i, title: `智能路线 #${i}`, image: `https://picsum.photos/seed/route${i}/400/400`})" class="text-xs font-bold text-emerald-600 flex items-center gap-1">
                      详情 <ChevronRight class="w-3 h-3" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Popular / Seasonal / Theme Lists -->
          <div v-else-if="['popular', 'seasonal', 'theme'].includes(planningMode)" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div v-if="planningMode === 'seasonal'" class="col-span-full flex gap-2 mb-2">
              <button v-for="s in ['春季', '夏季', '秋季', '冬季']" :key="s" @click="selectedSeason = s" :class="['px-3 py-1.5 rounded-full text-[10px] font-bold border transition-all', selectedSeason === s ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-white border-stone-200 text-stone-500']">
                {{ s }}
              </button>
            </div>
            <div v-if="planningMode === 'theme'" class="col-span-full flex gap-2 mb-2">
              <button v-for="t in ['文化', '自然', '美食', '摄影', '亲子']" :key="t" @click="selectedTheme = t" :class="['px-3 py-1.5 rounded-full text-[10px] font-bold border transition-all', selectedTheme === t ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-white border-stone-200 text-stone-500']">
                {{ t }}
              </button>
            </div>
            <div v-for="i in 6" :key="i" class="bg-white rounded-[2rem] border border-stone-200 shadow-sm overflow-hidden group hover:shadow-xl transition-all">
              <div class="relative h-40 overflow-hidden">
                <img :src="`https://picsum.photos/seed/plan${i}/600/400`" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
                <div class="absolute top-3 left-3 bg-white/90 backdrop-blur px-2 py-1 rounded-lg text-[9px] font-bold text-stone-900">
                  {{ planningMode === 'popular' ? '热门' : planningMode === 'seasonal' ? selectedSeason : selectedTheme }}
                </div>
              </div>
              <div class="p-4">
                <h4 class="text-sm font-bold text-stone-900 mb-1">精选{{ planningMode === 'popular' ? '热门' : '' }}路线 #{{ i }}</h4>
                <p class="text-[10px] text-stone-500 line-clamp-2 mb-3">深度探索城市的每一个角落，体验最地道的风土人情。</p>
                <div class="flex items-center justify-between">
                  <span class="text-[10px] font-bold text-emerald-600">质量评分: 4.9</span>
                  <button @click.stop="viewRecommendation({id: i+10, title: `精选路线 #${i}`, image: `https://picsum.photos/seed/plan${i}/600/400`})" class="text-[10px] font-bold text-stone-900 bg-stone-100 px-3 py-1.5 rounded-xl hover:bg-stone-200 transition-colors">查看详情</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Travel Guide Generation (New based on AIAdvancedController) -->
          <div v-else-if="planningMode === 'guide'" class="max-w-3xl mx-auto w-full space-y-8 py-8">
            <div class="text-center space-y-2">
              <h3 class="text-2xl font-serif font-bold text-stone-900">AI 智能攻略生成</h3>
              <p class="text-sm text-stone-500">输入城市和天数，AI 为您定制深度旅游攻略</p>
            </div>
            <div class="bg-white p-8 rounded-[32px] border border-stone-200 shadow-sm space-y-6">
              <div class="grid grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">目标城市</label>
                  <input type="text" placeholder="例如：京都" class="w-full bg-stone-50 border border-stone-200 rounded-2xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
                </div>
                <div class="space-y-2">
                  <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">游玩天数</label>
                  <input type="number" placeholder="例如：5" class="w-full bg-stone-50 border border-stone-200 rounded-2xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
                </div>
              </div>
              <button @click="handleGenerateGuide" class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20 flex items-center justify-center gap-2">
                <BookOpen class="w-5 h-5" /> 立即生成攻略
              </button>
            </div>
          </div>

          <!-- Budget Estimation (New based on AIAdvancedController) -->
          <div v-else-if="planningMode === 'budget'" class="max-w-3xl mx-auto w-full space-y-8 py-8">
            <div class="text-center space-y-2">
              <h3 class="text-2xl font-serif font-bold text-stone-900">AI 旅游预算估算</h3>
              <p class="text-sm text-stone-500">精准预估您的旅行开销，合理规划每一分钱</p>
            </div>
            <div class="bg-white p-8 rounded-[32px] border border-stone-200 shadow-sm space-y-6">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">目的地</label>
                  <input type="text" placeholder="例如：巴黎" class="w-full bg-stone-50 border border-stone-200 rounded-2xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
                </div>
                <div class="space-y-2">
                  <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">消费等级</label>
                  <select class="w-full bg-stone-50 border border-stone-200 rounded-2xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all">
                    <option>经济型</option>
                    <option>舒适型</option>
                    <option>奢华型</option>
                  </select>
                </div>
              </div>
              <button @click="handleEstimateBudget" class="w-full bg-amber-500 text-white py-4 rounded-2xl font-bold hover:bg-amber-600 transition-all shadow-lg shadow-amber-500/20 flex items-center justify-center gap-2">
                <Wallet class="w-5 h-5" /> 开始估算预算
              </button>
            </div>
          </div>

          <!-- AI Smart Planning (New based on AISmartItineraryService) -->
          <div v-else-if="planningMode === 'smart'" class="max-w-5xl mx-auto w-full space-y-8 py-8">
            <div class="flex justify-between items-end">
              <div>
                <h3 class="text-2xl font-serif font-bold text-stone-900">AI 智能行程生成器</h3>
                <p class="text-sm text-stone-500 mt-1">基于大数据与深度学习，为您生成最科学的旅行方案</p>
              </div>
              <div v-if="satisfactionPrediction !== null && satisfactionPrediction > 0" class="flex items-center gap-3 bg-emerald-50 px-4 py-2 rounded-2xl border border-emerald-100">
                <div class="text-right">
                  <p class="text-[10px] font-bold text-stone-400 uppercase">AI 预测满意度</p>
                  <p class="text-lg font-bold text-emerald-600">{{ satisfactionPrediction }}%</p>
                </div>
                <Activity class="w-6 h-6 text-emerald-600" />
              </div>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
              <!-- Preferences Sidebar -->
              <div class="lg:col-span-4 space-y-6">
                <div class="bg-white p-6 rounded-[32px] border border-stone-200 shadow-sm space-y-6">
                  <h4 class="text-xs font-bold text-stone-900 uppercase tracking-widest">智能偏好设置</h4>
                  
                  <div class="space-y-3">
                    <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">旅行节奏</label>
                    <div class="grid grid-cols-3 gap-2">
                      <button 
                        v-for="p in ['slow', 'moderate', 'fast']" 
                        :key="p"
                        @click="smartItineraryPreferences.pace = p as any"
                        :class="['py-2 rounded-xl text-[10px] font-bold border transition-all', smartItineraryPreferences.pace === p ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-stone-50 border-stone-100 text-stone-500']"
                      >
                        {{ p === 'slow' ? '悠闲' : p === 'moderate' ? '适中' : '特种兵' }}
                      </button>
                    </div>
                  </div>

                  <div class="space-y-3">
                    <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">住宿偏好</label>
                    <select v-model="smartItineraryPreferences.accommodationStyle" class="w-full bg-stone-50 border border-stone-200 rounded-xl px-3 py-2 text-[11px] font-bold outline-none focus:border-emerald-600">
                      <option value="boutique">精品酒店</option>
                      <option value="luxury">奢华度假村</option>
                      <option value="budget">经济民宿</option>
                      <option value="local">当地特色</option>
                    </select>
                  </div>

                  <div class="space-y-3">
                    <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">核心兴趣 (多选)</label>
                    <div class="flex flex-wrap gap-2">
                      <button 
                        v-for="interest in ['摄影', '美食', '历史', '自然', '购物', '艺术']" 
                        :key="interest"
                        @click="toggleInterest(interest)"
                        :class="['px-3 py-1.5 rounded-full text-[10px] font-bold border transition-all', smartItineraryPreferences.interests.includes(interest) ? 'bg-stone-900 border-stone-900 text-white' : 'bg-stone-50 border-stone-100 text-stone-500']"
                      >
                        {{ interest }}
                      </button>
                    </div>
                  </div>

                  <button @click="generateSmartItinerary" class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20 flex items-center justify-center gap-2">
                    <Sparkles class="w-5 h-5" /> 重新生成方案
                  </button>
                </div>
              </div>

              <!-- Generated Itinerary -->
              <div class="lg:col-span-8 space-y-6">
                <div v-if="alternativeItineraries.length > 0" class="space-y-4">
                  <div v-for="(alt, idx) in alternativeItineraries" :key="idx" class="bg-white p-6 rounded-[32px] border border-stone-200 shadow-sm hover:border-emerald-200 transition-all group">
                    <div class="flex justify-between items-start mb-4">
                      <div>
                        <div class="flex items-center gap-2 mb-1">
                          <span class="px-2 py-0.5 bg-emerald-100 text-emerald-700 text-[8px] font-bold rounded uppercase tracking-widest">方案 {{ idx + 1 }}</span>
                          <h4 class="text-lg font-bold text-stone-900">{{ alt.title }}</h4>
                        </div>
                        <p class="text-xs text-stone-500">{{ alt.description }}</p>
                      </div>
                      <div class="text-right">
                        <p class="text-sm font-bold text-emerald-600">¥{{ alt.estimatedCost }}</p>
                        <p class="text-[10px] text-stone-400">预估总价</p>
                      </div>
                    </div>
                    
                    <div class="flex items-center gap-6 py-4 border-y border-stone-50 mb-4">
                      <div class="flex items-center gap-2">
                        <Clock class="w-4 h-4 text-stone-400" />
                        <span class="text-xs font-bold text-stone-600">{{ alt.duration }}天</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <MapPin class="w-4 h-4 text-stone-400" />
                        <span class="text-xs font-bold text-stone-600">{{ alt.stops }} 个停留点</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <Activity class="w-4 h-4 text-stone-400" />
                        <span class="text-xs font-bold text-stone-600">匹配度 {{ alt.matchScore }}%</span>
                      </div>
                    </div>

                    <div class="flex justify-between items-center">
                      <div class="flex gap-2">
                        <span v-for="tag in alt.tags" :key="tag" class="text-[9px] font-bold text-stone-400 bg-stone-50 px-2 py-1 rounded-md">#{{ tag }}</span>
                      </div>
                      <button @click="selectItineraryPlan(alt)" class="bg-stone-900 text-white px-6 py-2 rounded-xl text-xs font-bold hover:bg-stone-800 transition-all">选择此方案</button>
                    </div>
                  </div>
                </div>
                <div v-else class="h-96 bg-stone-50 rounded-[32px] border-2 border-dashed border-stone-200 flex flex-col items-center justify-center text-stone-400 gap-4">
                  <div class="w-16 h-16 rounded-full bg-stone-100 flex items-center justify-center">
                    <Sparkles class="w-8 h-8" />
                  </div>
                  <p class="text-sm font-bold">点击左侧按钮开始智能规划</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="currentView === 'home'" class="max-w-7xl mx-auto h-full grid grid-cols-1 lg:grid-cols-12 gap-3 overflow-hidden">
        
        <!-- Left Column: Hero & Planner -->
        <div class="lg:col-span-8 flex flex-col gap-4 justify-center overflow-hidden">
          <Motion
            :initial="{ opacity: 0, x: -20 }"
            :animate="{ opacity: 1, x: 0 }"
            :transition="{ duration: 0.6 }"
            class="shrink-0 mb-2"
          >
            <span class="inline-block px-3 py-1 mb-2 text-[10px] font-bold tracking-widest text-emerald-700 uppercase bg-emerald-50 rounded-full border border-emerald-100">
              探索世界
            </span>
            <h1 class="text-3xl lg:text-5xl font-serif font-bold text-stone-900 leading-tight mb-2">
              规划您的下一次 <br />
              <span class="italic text-emerald-600">难忘</span> 旅程
            </h1>
            <p class="text-xs text-stone-500 max-w-lg leading-relaxed">
              一站式发现隐藏秘境、创建自定义行程并预订独特体验。
            </p>
          </Motion>

          <!-- Search Bar (Planner) -->
          <Motion 
            :initial="{ opacity: 0, y: 20 }"
            :animate="{ opacity: 1, y: 0 }"
            :transition="{ duration: 0.6, delay: 0.2 }"
            class="bg-white p-5 rounded-[2.5rem] shadow-xl shadow-stone-200/20 border border-stone-100 flex flex-col gap-4 shrink-0"
          >
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div class="flex items-center px-4 py-3 gap-3 bg-stone-50 rounded-2xl border border-stone-100">
                <MapPin class="text-emerald-600 w-5 h-5 flex-shrink-0" />
                <div class="flex flex-col flex-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-stone-400">目的地</span>
                  <input 
                    type="text" 
                    placeholder="想去哪里？" 
                    class="w-full bg-transparent outline-none text-sm text-stone-900 placeholder:text-stone-300 font-medium"
                    v-model="searchQuery"
                  />
                </div>
              </div>
              <div class="flex items-center px-4 py-3 gap-3 bg-stone-50 rounded-2xl border border-stone-100">
                <Calendar class="text-emerald-600 w-5 h-5 flex-shrink-0" />
                <div class="flex flex-col flex-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-stone-400">日期</span>
                  <input 
                    type="date" 
                    class="w-full bg-transparent outline-none text-sm text-stone-900 font-medium cursor-pointer"
                    v-model="selectedDate"
                  />
                </div>
              </div>
              <div class="flex items-center px-4 py-3 gap-3 bg-stone-50 rounded-2xl border border-stone-100">
                <Clock class="text-emerald-600 w-5 h-5 flex-shrink-0" />
                <div class="flex flex-col flex-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-stone-400">时长</span>
                  <div class="flex items-center gap-4">
                    <button @click="tripDuration = Math.max(1, tripDuration - 1)" class="text-stone-400 hover:text-emerald-600 font-bold text-lg">-</button>
                    <span class="text-stone-900 font-medium text-sm">{{ tripDuration }}天</span>
                    <button @click="tripDuration++" class="text-stone-400 hover:text-emerald-600 font-bold text-lg">+</button>
                  </div>
                </div>
              </div>
              <div class="flex items-center px-4 py-3 gap-3 bg-stone-50 rounded-2xl border border-stone-100">
                <Users class="text-emerald-600 w-5 h-5 flex-shrink-0" />
                <div class="flex flex-col flex-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-stone-400">人数</span>
                  <div class="flex items-center gap-4">
                    <button @click="guestCount = Math.max(1, guestCount - 1)" class="text-stone-400 hover:text-emerald-600 font-bold text-lg">-</button>
                    <span class="text-stone-900 font-medium text-sm">{{ guestCount }}</span>
                    <button @click="guestCount++" class="text-stone-400 hover:text-emerald-600 font-bold text-lg">+</button>
                  </div>
                </div>
              </div>

              <!-- Smart Planning Options (New) -->
              <div class="flex items-center px-4 py-3 gap-3 bg-stone-50 rounded-2xl border border-stone-100">
                <Wallet class="text-emerald-600 w-5 h-5 flex-shrink-0" />
                <div class="flex flex-col flex-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-stone-400">预算</span>
                  <div class="flex items-center gap-3">
                    <input type="range" min="500" max="10000" step="100" v-model="budgetRange" class="flex-1 accent-emerald-600 h-1.5" />
                    <span class="text-[11px] font-bold text-stone-700 w-10">¥{{ budgetRange/1000 }}k</span>
                  </div>
                </div>
              </div>
              <div class="flex items-center px-4 py-3 gap-3 bg-stone-50 rounded-2xl border border-stone-100">
                <Bus class="text-emerald-600 w-5 h-5 flex-shrink-0" />
                <div class="flex flex-col flex-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-stone-400">交通</span>
                  <select v-model="transportMode" class="w-full bg-transparent outline-none text-sm text-stone-900 font-medium">
                    <option value="mixed">混合</option>
                    <option value="public">公共交通</option>
                    <option value="private">私人包车</option>
                  </select>
                </div>
              </div>

              <button 
                @click="startPlanning"
                class="bg-emerald-600 text-white px-6 py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all flex items-center justify-center gap-3 shadow-lg shadow-emerald-600/20 sm:col-span-3 group text-sm"
              >
                <Sparkles class="w-5 h-5 group-hover:rotate-12 transition-transform" />
                <span>智能路线生成</span>
              </button>
            </div>

            <!-- Travel Styles -->
            <div class="border-t border-stone-50 pt-4">
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mr-2">风格:</span>
                <button 
                  v-for="style in TRAVEL_STYLES.slice(0, 6)" 
                  :key="style"
                  @click="addStyle(style)"
                  :class="[
                    'px-3.5 py-1.5 rounded-full text-[11px] font-medium transition-all border',
                    userStyles.includes(style) 
                      ? 'bg-emerald-600 border-emerald-600 text-white' 
                      : 'bg-stone-50 border-stone-200 text-stone-600'
                  ]"
                >
                  {{ style }}
                </button>
                <div class="flex items-center gap-2 ml-auto">
                  <input 
                    type="text" 
                    v-model="newStyle"
                    @keyup.enter="addNewCustomStyle"
                    placeholder="+"
                    class="w-10 text-[11px] bg-stone-50 border border-stone-200 rounded-full px-3 py-1.5 outline-none focus:w-32 transition-all"
                  />
                </div>
              </div>
            </div>
          </Motion>
        </div>

        <!-- Right Column: Recommendations & Real-time (Shrunk) -->
        <div class="lg:col-span-4 flex flex-col gap-3 overflow-hidden h-full">
          
          <!-- Real-time Alerts (New) -->
          <div class="bg-white p-3 rounded-2xl border border-stone-100 shadow-sm shrink-0">
            <div class="flex items-center justify-between mb-2">
              <h3 class="text-[10px] font-bold text-stone-900 flex items-center gap-1.5">
                <ShieldAlert class="w-3 h-3 text-amber-500" /> 实时提醒
              </h3>
              <span class="text-[8px] text-stone-400 uppercase tracking-widest">实时</span>
            </div>
            <div class="grid grid-cols-1 gap-2 max-h-[180px] overflow-y-auto no-scrollbar">
              <div v-for="alert in REALTIME_ALERTS" :key="alert.id" class="p-2 rounded-xl bg-stone-50 border border-stone-100 flex gap-2.5 items-center hover:bg-stone-100 transition-colors cursor-default">
                <div :class="[
                  'w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0', 
                  alert.severity === 'high' ? 'bg-red-100 text-red-600' : 
                  alert.severity === 'medium' ? 'bg-amber-100 text-amber-600' : 
                  'bg-blue-100 text-blue-600'
                ]">
                  <component :is="alert.type === 'weather' ? Sparkles : alert.type === 'crowd' ? Users : alert.type === 'traffic' ? Bus : ShieldAlert" class="w-3.5 h-3.5" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-[10px] font-bold text-stone-900 truncate">{{ alert.title }}</p>
                  <p class="text-[9px] text-stone-500 truncate">{{ alert.description }}</p>
                </div>
                <ChevronRight class="w-3 h-3 text-stone-300" />
              </div>
            </div>
          </div>
          
          <!-- 精选推荐与热门目的地 (Combined Section) -->
          <div class="flex-1 min-h-0 flex flex-col bg-white p-3 rounded-2xl border border-stone-100 shadow-sm overflow-hidden">
            <div class="flex justify-between items-center mb-2 shrink-0">
              <h2 class="text-xs font-bold text-stone-900 flex items-center gap-2">
                <Sparkles class="w-3.5 h-3.5 text-emerald-600" /> 精选推荐与热门
              </h2>
            </div>
            
            <div class="flex-1 overflow-y-auto no-scrollbar space-y-3">
              <!-- 推荐卡片 (Horizontal Scroll or Grid) -->
              <div class="grid grid-cols-2 gap-2.5">
                <Motion 
                  v-for="(rec, index) in RECOMMENDATIONS"
                  :key="rec.id"
                  :initial="{ opacity: 0, y: 10 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :transition="{ duration: 0.4, delay: index * 0.05 }"
                  class="group cursor-pointer"
                  @click="viewRecommendation(rec)"
                >
                  <div class="relative h-24 rounded-xl overflow-hidden mb-1.5 shadow-sm">
                    <img :src="rec.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" referrerPolicy="no-referrer" />
                    <div class="absolute top-1.5 left-1.5 flex gap-1">
                      <span v-for="tag in rec.tags.slice(0, 1)" :key="tag" class="bg-white/90 backdrop-blur px-1.5 py-0.5 rounded text-[8px] font-bold text-emerald-700 uppercase">
                        {{ tag }}
                      </span>
                    </div>
                  </div>
                  <h4 class="text-[11px] font-bold text-stone-900 truncate leading-tight">{{ rec.title }}</h4>
                  <p class="text-[9px] text-stone-500 flex items-center gap-1 mt-0.5">
                    <MapPin class="w-2.5 h-2.5" /> {{ rec.location }}
                  </p>
                </Motion>

                <!-- 热门目的地卡片 (Integrated into same grid) -->
                <Motion 
                  v-for="(dest, index) in DESTINATIONS"
                  :key="dest.id"
                  :initial="{ opacity: 0, y: 10 }"
                  :animate="{ opacity: 1, y: 0 }"
                  :transition="{ duration: 0.4, delay: (index + RECOMMENDATIONS.length) * 0.05 }"
                  class="group cursor-pointer"
                  @click="viewRecommendation(dest)"
                >
                  <div class="relative h-24 rounded-xl overflow-hidden mb-1.5 shadow-sm">
                    <img :src="dest.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" referrerPolicy="no-referrer" />
                    <div class="absolute bottom-1.5 left-1.5 right-1.5">
                      <div class="bg-black/40 backdrop-blur-sm px-2 py-1 rounded-lg flex justify-between items-center">
                        <span class="text-[9px] font-bold text-white truncate">{{ dest.name }}</span>
                        <span class="text-[9px] font-bold text-emerald-400">{{ dest.price }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="flex justify-between items-center">
                    <span class="text-[9px] font-medium text-stone-400 uppercase tracking-tighter">{{ dest.category }}</span>
                    <div class="flex items-center gap-0.5">
                      <Star class="w-2 h-2 fill-amber-400 text-amber-400" />
                      <span class="text-[9px] font-bold text-stone-600">{{ dest.rating }}</span>
                    </div>
                  </div>
                </Motion>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- Community View (New) -->
      <div v-else-if="currentView === 'community'" class="max-w-7xl mx-auto h-full overflow-y-auto custom-scrollbar pb-20 p-4">
        <div class="flex flex-col gap-8">
          <div class="flex flex-col md:flex-row justify-between items-center gap-6">
            <div class="w-full md:w-auto">
              <h2 class="text-3xl font-serif font-bold text-stone-900">旅行社区</h2>
              <p class="text-sm text-stone-500 mt-1">从 {{ platformOverview.totalUsers }} 位旅行者的故事中获取灵感。</p>
            </div>
            
            <div class="flex flex-1 w-full max-w-xl items-center gap-3">
              <div class="flex-1 relative">
                <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
                <input 
                  type="text" 
                  placeholder="搜索游记、地点、用户..." 
                  class="w-full bg-white border border-stone-200 rounded-2xl py-3 pl-10 pr-4 text-sm focus:border-emerald-600 focus:ring-1 focus:ring-emerald-100 transition-all outline-none"
                />
              </div>
              <button 
                @click="showPostNoteModal = true"
                class="bg-emerald-600 text-white px-6 py-3 rounded-2xl text-sm font-bold flex items-center gap-2 hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-100 shrink-0"
              >
                <Camera class="w-4 h-4" /> 发布笔记
              </button>
            </div>
          </div>

          <!-- Travel Notes Grid -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div v-for="note in travelNotes" :key="note.id" class="bg-white rounded-3xl border border-stone-100 overflow-hidden shadow-sm hover:shadow-xl transition-all group">
              <div class="relative h-56 overflow-hidden cursor-pointer" @click="viewRecommendation(note)">
                <img :src="note.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" referrerPolicy="no-referrer" />
                <div class="absolute top-4 left-4 bg-white/90 backdrop-blur px-3 py-1 rounded-full text-[10px] font-bold text-stone-900">
                  旅行笔记
                </div>
                <button
                    @click.stop="likeNote(note)"
                    :class="['absolute top-4 right-4 w-8 h-8 rounded-full flex items-center justify-center transition-colors', note.isLiked ? 'bg-red-500 text-white' : 'bg-white/90 backdrop-blur text-stone-600 hover:text-red-500']"
                >
                  <Heart :class="['w-4 h-4', note.isLiked ? 'fill-current' : '']" />
                </button>
              </div>
              <div class="p-6">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-6 h-6 rounded-full bg-stone-200 overflow-hidden">
                    <img src="https://picsum.photos/seed/user/100/100" class="w-full h-full object-cover" referrerPolicy="no-referrer" />
                  </div>
                  <span class="text-[10px] font-bold text-stone-500">{{ note.author }}</span>
                </div>
                <h3 @click="viewRecommendation(note)" class="text-lg font-bold text-stone-900 mb-2 group-hover:text-emerald-600 transition-colors cursor-pointer">{{ note.title }}</h3>
                <p class="text-xs text-stone-500 line-clamp-2 mb-4">{{ note.excerpt }}</p>
                <div class="flex items-center justify-between pt-4 border-t border-stone-50">
                  <div class="flex items-center gap-4">
                    <button @click="likeNote(note)" class="flex items-center gap-1.5 transition-colors" :class="note.isLiked ? 'text-red-500' : 'text-stone-400 hover:text-red-500'">
                      <ThumbsUp class="w-3.5 h-3.5" :class="note.isLiked ? 'fill-current' : ''" />
                      <span class="text-[10px] font-bold">{{ note.likes }}</span>
                    </button>

                    <button @click="openComments(note)" class="flex items-center gap-1.5 text-stone-400 hover:text-emerald-600 transition-colors">
                      <MessageSquare class="w-3.5 h-3.5" />
                      <span class="text-[10px] font-bold">{{ note.comments }}</span>
                    </button>
                  </div>
                  <div class="flex items-center gap-2">
                    <div class="px-2 py-0.5 bg-emerald-50 text-emerald-600 rounded-md text-[8px] font-bold flex items-center gap-1">
                      <Sparkles class="w-2 h-2" /> AI 情感: 积极
                    </div>
                    <button @click="toggleCollection(note)" class="p-1.5 text-stone-400 hover:text-amber-500 transition-colors" :title="note.isCollected ? '取消收藏' : '收藏'">
                      <Star :class="['w-4 h-4', note.isCollected ? 'fill-amber-400 text-amber-400' : '']" />
                    </button>
                    <button @click="generateShareCode(note)" class="text-stone-400 hover:text-emerald-600 transition-colors" title="分享笔记">
                      <Share2 class="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Itineraries View -->
      <div v-else-if="currentView === 'itineraries'" class="max-w-7xl mx-auto h-full flex flex-col gap-6">
        <Motion 
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          class="flex-1 flex flex-col gap-6 overflow-hidden"
        >
          <div class="flex justify-between items-center">
            <div>
              <h2 class="text-2xl font-serif font-bold text-stone-900">我的行程规划</h2>
              <p class="text-xs text-stone-500 mt-1">管理并查看您即将开始的旅行计划</p>
            </div>
            <div class="flex items-center gap-3">
              <button @click="openAnalytics" class="p-2.5 rounded-2xl bg-stone-100 text-stone-600 hover:bg-stone-200 transition-all" title="平台统计">
                <BarChart3 class="w-4 h-4" />
              </button>
              <button class="bg-stone-900 text-white px-6 py-2.5 rounded-2xl text-xs font-bold hover:bg-stone-800 transition-all shadow-lg flex items-center gap-2">
                <Compass class="w-4 h-4" /> 创建新行程
              </button>
            </div>
          </div>

          <!-- Itineraries Grid -->
          <div class="flex-1 overflow-y-auto scrollbar-hide">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pb-6">
              <div 
                v-for="itinerary in PLANNED_ITINERARIES" 
                :key="itinerary.id" 
                @click="viewItinerary(itinerary)"
                class="bg-white rounded-[32px] border border-stone-200 overflow-hidden shadow-sm hover:shadow-md transition-all group cursor-pointer"
              >
                <div class="relative h-48 overflow-hidden">
                  <img :src="itinerary.image" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" referrerPolicy="no-referrer" />
                  <div class="absolute top-4 right-4">
                    <span :class="[
                      'px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest border backdrop-blur-md',
                      itinerary.status === '已确认' ? 'bg-emerald-50/80 text-emerald-700 border-emerald-100' : 
                      itinerary.status === '规划中' ? 'bg-blue-50/80 text-blue-700 border-blue-100' : 
                      'bg-stone-50/80 text-stone-700 border-stone-100'
                    ]">
                      {{ itinerary.status }}
                    </span>
                  </div>
                </div>
                <div class="p-6">
                  <div class="flex justify-between items-start mb-4">
                    <div>
                      <h3 class="text-lg font-serif font-bold text-stone-900">{{ itinerary.title }}</h3>
                      <p class="text-xs text-stone-500 flex items-center gap-1 mt-1">
                        <MapPin class="w-3 h-3" /> {{ itinerary.destination }}
                      </p>
                    </div>
                  </div>
                  
                  <div class="grid grid-cols-2 gap-4 mb-4">
                    <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                      <div class="flex items-center gap-2 text-stone-400 mb-1">
                        <Calendar class="w-3 h-3" />
                        <span class="text-[9px] font-bold uppercase tracking-tighter">时长</span>
                      </div>
                      <span class="text-xs font-bold text-stone-900">{{ itinerary.days }} 天</span>
                    </div>
                    <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                      <div class="flex items-center gap-2 text-stone-400 mb-1">
                        <Navigation class="w-3 h-3" />
                        <span class="text-[9px] font-bold uppercase tracking-tighter">活动</span>
                      </div>
                      <span class="text-xs font-bold text-stone-900">{{ itinerary.activities }} 景点</span>
                    </div>
                  </div>

                  <!-- Completion Rate & Collaborators (New) -->
                  <div class="space-y-3 mb-6">
                    <div class="flex justify-between items-center">
                      <div class="flex items-center gap-2">
                        <Activity class="w-3 h-3 text-emerald-600" />
                        <span class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">完成率</span>
                      </div>
                      <span class="text-[10px] font-bold text-emerald-600">{{ itinerary.completionRate }}%</span>
                    </div>
                    <div class="w-full h-1.5 bg-stone-100 rounded-full overflow-hidden">
                      <div class="h-full bg-emerald-600 transition-all duration-1000" :style="{ width: `${itinerary.completionRate}%` }"></div>
                    </div>
                    <div class="flex justify-between items-center">
                      <div class="flex -space-x-2">
                        <div v-for="i in itinerary.collaborators" :key="i" class="w-6 h-6 rounded-full border-2 border-white bg-stone-200 overflow-hidden">
                          <img :src="`https://picsum.photos/seed/user${i}/100/100`" class="w-full h-full object-cover" />
                        </div>
                        <button @click.stop="openCollaboration(itinerary)" class="w-6 h-6 rounded-full border-2 border-white bg-emerald-50 flex items-center justify-center text-emerald-600 hover:bg-emerald-100 transition-colors">
                          <UserPlus class="w-3 h-3" />
                        </button>
                      </div>
                      <div class="flex gap-2">
                        <button @click.stop="toggleCollection(itinerary)" class="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 transition-colors" :title="itinerary.isCollected ? '取消收藏' : '收藏行程'">
                          <Star :class="['w-3.5 h-3.5', itinerary.isCollected ? 'fill-amber-400 text-amber-400' : '']" />
                        </button>
                        <button @click.stop="generateShareCode(itinerary)" class="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 transition-colors" title="分享行程">
                          <Share2 class="w-3.5 h-3.5" />
                        </button>
                        <button @click.stop="toggleVisibility(itinerary)" class="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 transition-colors" :title="itinerary.isPublic ? '公开' : '私有'">
                          <Eye v-if="itinerary.isPublic" class="w-3.5 h-3.5 text-emerald-600" />
                          <EyeOff v-else class="w-3.5 h-3.5" />
                        </button>
                        <button @click.stop="copyRoute(itinerary)" class="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 transition-colors" title="复制路线">
                          <Copy class="w-3.5 h-3.5" />
                        </button>
                        <button @click.stop="deleteRoute(itinerary.id)" class="p-1.5 rounded-lg hover:bg-stone-100 text-red-400 transition-colors" title="删除路线">
                          <Trash2 class="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <div class="flex items-center justify-between pt-4 border-t border-stone-100">
                    <div class="flex items-center gap-2">
                      <Clock class="w-3.5 h-3.5 text-stone-400" />
                      <span class="text-[10px] text-stone-500 font-medium">{{ itinerary.dates }}</span>
                    </div>
                    <div class="flex gap-2">
                      <button 
                        @click.stop="getAdjustment(itinerary)"
                        class="px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-xl text-[10px] font-bold hover:bg-emerald-100 transition-colors flex items-center gap-1"
                      >
                        <Navigation class="w-3 h-3" /> 实时调整
                      </button>
                      <button 
                        @click.stop="viewItinerary(itinerary)"
                        class="w-8 h-8 bg-stone-900 text-white rounded-full flex items-center justify-center hover:bg-emerald-600 transition-colors"
                      >
                        <ArrowRight class="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Motion>
      </div>

      <!-- Profile View -->
      <div v-else-if="currentView === 'profile'" class="max-w-7xl mx-auto h-full flex flex-col gap-6">
        <Motion 
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          class="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6 overflow-hidden"
        >
          <!-- Profile Sidebar -->
          <div class="lg:col-span-4 flex flex-col gap-6">
            <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm flex flex-col items-center text-center">
              <div class="relative mb-4">
                <img src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=2070&auto=format&fit=crop" class="w-24 h-24 rounded-full object-cover border-4 border-emerald-50 shadow-lg" referrerPolicy="no-referrer" />
                <div class="absolute bottom-0 right-0 w-6 h-6 bg-emerald-600 rounded-full border-2 border-white flex items-center justify-center">
                  <Camera class="text-white w-3 h-3" />
                </div>
              </div>
              <h2 class="text-xl font-serif font-bold text-stone-900">亚历克斯·汤普森</h2>
              <p class="text-xs text-stone-500 mb-6">冒险追求者 & 美食家 | 24个国家</p>
              
              <div class="grid grid-cols-3 w-full gap-2 mb-6">
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-sm font-bold text-stone-900">12</span>
                  <span class="text-[10px] text-stone-400 uppercase font-bold tracking-tighter">旅行</span>
                </div>
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-sm font-bold text-stone-900">48</span>
                  <span class="text-[10px] text-stone-400 uppercase font-bold tracking-tighter">已收藏</span>
                </div>
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-sm font-bold text-stone-900">3.2k</span>
                  <span class="text-[10px] text-stone-400 uppercase font-bold tracking-tighter">里程</span>
                </div>
              </div>

              <div class="w-full space-y-2">
                <button class="w-full flex items-center justify-between p-3 rounded-2xl bg-emerald-50 text-emerald-700 border border-emerald-100 hover:bg-emerald-100 transition-colors">
                  <div class="flex items-center gap-3">
                    <User class="w-4 h-4" />
                    <span class="text-xs font-bold">个人信息</span>
                  </div>
                  <ChevronRight class="w-4 h-4" />
                </button>
                <button class="w-full flex items-center justify-between p-3 rounded-2xl text-stone-600 hover:bg-stone-50 transition-colors">
                  <div class="flex items-center gap-3">
                    <Settings class="w-4 h-4" />
                    <span class="text-xs font-bold">安全设置</span>
                  </div>
                  <ChevronRight class="w-4 h-4" />
                </button>
                <button class="w-full flex items-center justify-between p-3 rounded-2xl text-stone-600 hover:bg-stone-50 transition-colors">
                  <div class="flex items-center gap-3">
                    <CreditCard class="w-4 h-4" />
                    <span class="text-xs font-bold">支付方式</span>
                  </div>
                  <ChevronRight class="w-4 h-4" />
                </button>
                <button @click="handleLogout" class="w-full flex items-center justify-between p-3 rounded-2xl text-red-600 hover:bg-red-50 transition-colors">
                  <div class="flex items-center gap-3">
                    <LogOut class="w-4 h-4" />
                    <span class="text-xs font-bold">退出登录</span>
                  </div>
                </button>
              </div>
            </div>
          </div>

          <!-- Profile Content -->
          <div class="lg:col-span-8 flex flex-col gap-6 overflow-hidden">
            <!-- Upcoming Trips -->
            <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm flex-1 overflow-hidden flex flex-col">
              <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-serif font-bold text-stone-900">即将到来的行程</h3>
                <button @click="handleViewMap" class="text-xs font-bold text-emerald-600 flex items-center gap-1">
                  <Map class="w-4 h-4" /> 查看地图
                </button>
              </div>
              <div class="space-y-4 overflow-y-auto scrollbar-hide flex-1">
                <div v-for="i in 2" :key="i" class="flex items-center gap-4 p-4 rounded-2xl bg-stone-50 border border-stone-100 group cursor-pointer hover:border-emerald-200 transition-all">
                  <div class="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0">
                    <img :src="DESTINATIONS[i].image" class="w-full h-full object-cover" referrerPolicy="no-referrer" />
                  </div>
                  <div class="flex-1">
                    <div class="flex justify-between items-start">
                      <div>
                        <h4 class="text-sm font-bold text-stone-900">{{ DESTINATIONS[i].name }}</h4>
                        <p class="text-[10px] text-stone-500 mt-1">4月12日 - 4月19日 • 2位访客</p>
                      </div>
                      <span class="px-2 py-1 bg-emerald-100 text-emerald-700 text-[9px] font-bold rounded-full uppercase tracking-wider">已确认</span>
                    </div>
                  </div>
                  <ArrowRight class="w-4 h-4 text-stone-300 group-hover:text-emerald-600 transition-colors" />
                </div>
              </div>
            </div>

            <!-- Travel Preferences -->
            <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm">
              <h3 class="text-lg font-serif font-bold text-stone-900 mb-4">旅行偏好</h3>
              <div class="grid grid-cols-2 gap-4">
                <div class="space-y-3">
                  <span class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">喜欢的风格</span>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="style in ['冒险', '奢华', '自然']" :key="style" class="px-3 py-1.5 bg-stone-50 text-stone-600 text-[10px] font-bold rounded-full border border-stone-200">
                      {{ style }}
                    </span>
                  </div>
                </div>
                <div class="space-y-3">
                  <span class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">饮食要求</span>
                  <div class="flex flex-wrap gap-2">
                    <span class="px-3 py-1.5 bg-stone-50 text-stone-600 text-[10px] font-bold rounded-full border border-stone-200">素食</span>
                    <span class="px-3 py-1.5 bg-stone-50 text-stone-600 text-[10px] font-bold rounded-full border border-stone-200">无麸质</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- API System Dashboard (New based on controller) -->
            <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm">
              <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-serif font-bold text-stone-900">API 系统状态</h3>
                <button 
                  @click="syncAllData"
                  :disabled="SYSTEM_STATS.health.status === 'syncing'"
                  class="px-3 py-1 bg-emerald-600 text-white rounded-lg text-[10px] font-bold hover:bg-emerald-700 transition-all disabled:opacity-50"
                >
                  {{ SYSTEM_STATS.health.status === 'syncing' ? '同步中...' : '立即同步数据' }}
                </button>
              </div>
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-[10px] text-stone-400 uppercase font-bold mb-1">数据库</span>
                  <span class="text-xs font-bold text-emerald-600">已连接</span>
                </div>
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-[10px] text-stone-400 uppercase font-bold mb-1">AI 服务</span>
                  <span class="text-xs font-bold text-emerald-600">可用</span>
                </div>
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-[10px] text-stone-400 uppercase font-bold mb-1">响应时间</span>
                  <span class="text-xs font-bold text-stone-900">23ms</span>
                </div>
                <div class="bg-stone-50 p-3 rounded-2xl border border-stone-100">
                  <span class="block text-[10px] text-stone-400 uppercase font-bold mb-1">可用率</span>
                  <span class="text-xs font-bold text-stone-900">99.9%</span>
                </div>
              </div>
              <div class="space-y-3">
                <span class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">热门 API 接口</span>
                <div class="space-y-2">
                  <div class="flex justify-between items-center p-2 bg-stone-50 rounded-xl border border-stone-100">
                    <span class="text-[10px] font-mono text-stone-600">/api/ai/advanced/chatbot</span>
                    <span class="text-[10px] font-bold text-stone-400">5,678 次调用</span>
                  </div>
                  <div class="flex justify-between items-center p-2 bg-stone-50 rounded-xl border border-stone-100">
                    <span class="text-[10px] font-mono text-stone-600">/api/intelligent-route/plan</span>
                    <span class="text-[10px] font-bold text-stone-400">4,567 次调用</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Motion>
      </div>

      <!-- Resource Management View (New based on ResourceFile, FileCategory, FileVersion Controllers) -->
      <div v-else-if="currentView === 'resources'" class="max-w-7xl mx-auto h-full flex flex-col gap-6 overflow-hidden">
        <div class="flex justify-between items-center shrink-0">
          <div>
            <h2 class="text-2xl font-serif font-bold text-stone-900">旅行资源库</h2>
            <p class="text-xs text-stone-500 mt-1">管理您的旅行文档、签证、照片及版本历史</p>
          </div>
          <div class="flex gap-2">
            <button @click="showFileUploadModal = true" class="bg-emerald-600 text-white px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 shadow-lg">
              <FileUp class="w-4 h-4" /> 上传新文件
            </button>
            <button @click="currentView = 'home'" class="bg-white border border-stone-200 text-stone-600 px-4 py-2 rounded-xl text-xs font-bold">返回首页</button>
          </div>
        </div>

        <div class="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6 overflow-hidden">
          <!-- Sidebar: Categories -->
          <div class="lg:col-span-3 flex flex-col gap-6 overflow-hidden">
            <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm flex flex-col overflow-hidden">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-sm font-bold text-stone-900">文件分类</h3>
                <button @click="handleAddCategory" class="text-emerald-600"><FolderPlus class="w-4 h-4" /></button>
              </div>
              
              <div class="space-y-1 overflow-y-auto scrollbar-hide">
                <button 
                  @click="selectedCategory = null"
                  :class="['w-full flex items-center justify-between p-3 rounded-xl transition-all', selectedCategory === null ? 'bg-emerald-50 text-emerald-700' : 'text-stone-600 hover:bg-stone-50']"
                >
                  <div class="flex items-center gap-3">
                    <Package class="w-4 h-4" />
                    <span class="text-xs font-bold">全部文件</span>
                  </div>
                  <span class="text-[10px] font-bold opacity-50">{{ resourceFiles.length }}</span>
                </button>
                
                <div v-for="cat in fileCategories.filter(c => !c.parentId)" :key="cat.id" class="space-y-1">
                  <button 
                    @click="selectedCategory = cat.tagName"
                    :class="['w-full flex items-center justify-between p-3 rounded-xl transition-all', selectedCategory === cat.tagName ? 'bg-emerald-50 text-emerald-700' : 'text-stone-600 hover:bg-stone-50']"
                  >
                    <div class="flex items-center gap-3">
                      <component :is="cat.tagName === '文档' ? FileText : Camera" class="w-4 h-4" />
                      <span class="text-xs font-bold">{{ cat.tagName }}</span>
                    </div>
                    <span class="text-[10px] font-bold opacity-50">{{ resourceFiles.filter(f => f.category === cat.tagName).length }}</span>
                  </button>
                  
                  <!-- Subcategories -->
                  <div class="pl-6 space-y-1">
                    <button 
                      v-for="sub in fileCategories.filter(c => c.parentId === cat.id)" 
                      :key="sub.id"
                      @click="selectedCategory = sub.tagName"
                      :class="['w-full flex items-center justify-between p-2 rounded-lg transition-all text-[11px]', selectedCategory === sub.tagName ? 'text-emerald-600 font-bold' : 'text-stone-400 hover:text-stone-600']"
                    >
                      <span>{{ sub.tagName }}</span>
                    </button>
                  </div>
                </div>
              </div>

              <div class="mt-auto pt-6 border-t border-stone-100">
                <div class="bg-stone-50 p-4 rounded-2xl">
                  <div class="flex justify-between items-center mb-2">
                    <span class="text-[10px] font-bold text-stone-400 uppercase">存储空间</span>
                    <span class="text-[10px] font-bold text-stone-900">4.2MB / 100MB</span>
                  </div>
                  <div class="h-1.5 bg-stone-200 rounded-full overflow-hidden">
                    <div class="h-full bg-emerald-500" style="width: 4.2%"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Main Content: File List -->
          <div class="lg:col-span-9 flex flex-col gap-6 overflow-hidden">
            <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm flex-1 flex flex-col overflow-hidden">
              <div class="flex flex-col md:flex-row justify-between items-center gap-4 mb-6">
                <div class="relative w-full md:w-96">
                  <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
                  <input 
                    v-model="fileSearchQuery"
                    type="text" 
                    placeholder="搜索文件名、描述或标签..." 
                    class="w-full pl-10 pr-4 py-2 bg-stone-50 border border-stone-100 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 transition-all"
                  />
                </div>
                <div class="flex items-center gap-2">
                  <button @click="handleShowStats" class="p-2 text-stone-400 hover:text-stone-600"><BarChart3 class="w-4 h-4" /></button>
                  <button @click="handleFileSettings" class="p-2 text-stone-400 hover:text-stone-600"><Settings class="w-4 h-4" /></button>
                </div>
              </div>

              <div class="flex-1 overflow-y-auto scrollbar-hide">
                <table class="w-full text-left border-collapse">
                  <thead>
                    <tr class="border-b border-stone-100">
                      <th class="pb-4 text-[10px] font-bold text-stone-400 uppercase tracking-widest">文件名</th>
                      <th class="pb-4 text-[10px] font-bold text-stone-400 uppercase tracking-widest">分类</th>
                      <th class="pb-4 text-[10px] font-bold text-stone-400 uppercase tracking-widest">大小</th>
                      <th class="pb-4 text-[10px] font-bold text-stone-400 uppercase tracking-widest">版本</th>
                      <th class="pb-4 text-[10px] font-bold text-stone-400 uppercase tracking-widest">最后修改</th>
                      <th class="pb-4 text-[10px] font-bold text-stone-400 uppercase tracking-widest text-right">操作</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-stone-50">
                    <tr v-for="file in resourceFiles" :key="file.id" class="group hover:bg-stone-50/50 transition-colors">
                      <td class="py-4">
                        <div class="flex items-center gap-3">
                          <div class="w-10 h-10 rounded-xl bg-stone-100 flex items-center justify-center flex-shrink-0 group-hover:bg-white transition-colors">
                            <FileText v-if="file.category === '文档'" class="w-5 h-5 text-stone-500" />
                            <Camera v-else class="w-5 h-5 text-stone-500" />
                          </div>
                          <div>
                            <p class="text-xs font-bold text-stone-900">{{ file.fileName }}</p>
                            <p class="text-[10px] text-stone-400 mt-0.5">{{ file.description }}</p>
                          </div>
                        </div>
                      </td>
                      <td class="py-4">
                        <span class="px-2 py-1 bg-stone-100 text-stone-600 text-[9px] font-bold rounded-lg uppercase">{{ file.category }}</span>
                      </td>
                      <td class="py-4 text-[11px] text-stone-500">{{ file.size }}</td>
                      <td class="py-4">
                        <button @click="activeFileForVersions = file; showVersionHistoryModal = true" class="flex items-center gap-1 text-[11px] font-bold text-emerald-600 hover:underline">
                          <History class="w-3 h-3" /> {{ file.version }}
                        </button>
                      </td>
                      <td class="py-4 text-[11px] text-stone-500">{{ file.uploadTime }}</td>
                       <td class="py-4 text-right">
                        <div class="flex items-center justify-end gap-2 px-2">
                          <button @click="generateShareCode(file)" class="p-2 text-stone-400 hover:text-emerald-600 transition-colors" title="分享文件">
                            <Share2 class="w-4 h-4" />
                          </button>
                          <button @click="handleDownloadFile(file)" class="p-2 text-stone-400 hover:text-emerald-600 transition-colors" title="下载文件">
                          <Download class="w-4 h-4" />
                          </button>
                          <button @click="handleDeleteFile(file.id)" class="p-2 text-stone-400 hover:text-red-600 transition-colors" title="删除文件">
                            <Trash2 class="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Recommendation Detail View -->
      <div v-else-if="currentView === 'detail' && selectedRecommendation" class="max-w-7xl mx-auto h-full flex flex-col gap-6">
        <Motion 
          :initial="{ opacity: 0, scale: 0.98 }"
          :animate="{ opacity: 1, scale: 1 }"
          class="flex-1 bg-white rounded-[32px] border border-stone-200 shadow-sm overflow-hidden flex flex-col"
        >
          <!-- Detail Header -->
          <div class="relative h-48 md:h-64 overflow-hidden">
            <img :src="selectedRecommendation.image" class="w-full h-full object-cover" referrerPolicy="no-referrer" />
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"></div>
            <button 
              @click="currentView = previousView"
              class="absolute top-4 left-4 w-8 h-8 bg-white shadow-lg rounded-full flex items-center justify-center text-stone-900 hover:bg-stone-50 transition-all z-10"
            >
              <ArrowLeft class="w-4 h-4" />
            </button>
            <div class="absolute top-4 right-4 flex gap-2 z-10">
              <button 
                @click="toggleCollection(selectedRecommendation)"
                class="w-10 h-10 bg-white shadow-lg rounded-full flex items-center justify-center text-stone-900 hover:bg-stone-50 transition-all"
                :title="selectedRecommendation.isCollected ? '取消收藏' : '收藏目的地'"
              >
                <Star :class="['w-4 h-4', selectedRecommendation.isCollected ? 'fill-amber-400 text-amber-400' : '']" />
              </button>
              <button 
                @click="generateShareCode(selectedRecommendation)"
                class="w-10 h-10 bg-white shadow-lg rounded-full flex items-center justify-center text-stone-900 hover:bg-stone-50 transition-all"
                title="分享目的地"
              >
                <Share2 class="w-4 h-4" />
              </button>
            </div>
            <div class="absolute bottom-4 left-6 right-6">
              <div class="flex items-center gap-2 mb-1.5">
                <span v-for="tag in selectedRecommendation.tags" :key="tag" class="text-[8px] font-bold uppercase tracking-widest text-emerald-400 bg-black/40 backdrop-blur-md px-2 py-0.5 rounded-full">
                  {{ tag }}
                </span>
              </div>
              <h1 class="text-2xl md:text-3xl font-serif font-bold text-white">{{ selectedRecommendation.title }}</h1>
              <p class="text-white/80 flex items-center gap-1.5 mt-1 text-xs">
                <MapPin class="w-3 h-3" /> {{ selectedRecommendation.location }}
              </p>
            </div>
          </div>

          <!-- Detail Content -->
          <div class="flex-1 overflow-y-auto p-8 md:p-12 scrollbar-hide">
            <div class="grid grid-cols-1 lg:grid-cols-12 gap-12">
              <div class="lg:col-span-8 space-y-8">
                <div>
                  <h3 class="text-xl font-serif font-bold text-stone-900 mb-4">关于此目的地</h3>
                  <p class="text-stone-600 leading-relaxed text-sm md:text-base">
                    {{ selectedRecommendation.description }}
                  </p>
                </div>

                <div>
                  <h3 class="text-xl font-serif font-bold text-stone-900 mb-4">体验亮点</h3>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div v-for="highlight in selectedRecommendation.highlights" :key="highlight" class="flex items-center gap-3 p-4 rounded-2xl bg-stone-50 border border-stone-100">
                      <div class="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0">
                        <Check class="w-4 h-4 text-emerald-600" />
                      </div>
                      <span class="text-sm font-medium text-stone-700">{{ highlight }}</span>
                    </div>
                  </div>
                </div>

                <!-- Route Evaluation & Optimization (New based on IntelligentRouteController) -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
                  <div class="bg-emerald-50/50 p-6 rounded-3xl border border-emerald-100">
                    <h4 class="text-sm font-bold text-emerald-900 mb-3 flex items-center gap-2">
                      <Sparkles class="w-4 h-4" /> 路线质量评估
                    </h4>
                    <div class="space-y-3">
                      <div class="flex justify-between items-center">
                        <span class="text-xs text-emerald-700">合理性</span>
                        <div class="w-24 h-1.5 bg-emerald-200 rounded-full overflow-hidden">
                          <div class="w-[95%] h-full bg-emerald-600"></div>
                        </div>
                      </div>
                      <div class="flex justify-between items-center">
                        <span class="text-xs text-emerald-700">丰富度</span>
                        <div class="w-24 h-1.5 bg-emerald-200 rounded-full overflow-hidden">
                          <div class="w-[88%] h-full bg-emerald-600"></div>
                        </div>
                      </div>
                      <div class="flex justify-between items-center">
                        <span class="text-xs text-emerald-700">舒适度</span>
                        <div class="w-24 h-1.5 bg-emerald-200 rounded-full overflow-hidden">
                          <div class="w-[92%] h-full bg-emerald-600"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="bg-amber-50/50 p-6 rounded-3xl border border-amber-100">
                    <h4 class="text-sm font-bold text-amber-900 mb-3 flex items-center gap-2">
                      <Navigation class="w-4 h-4" /> 优化建议
                    </h4>
                    <ul class="space-y-2">
                      <li class="text-[11px] text-amber-700 flex items-start gap-2">
                        <div class="w-1 h-1 rounded-full bg-amber-400 mt-1.5 shrink-0"></div>
                        建议将下午的景点顺序调整，以避开人流高峰。
                      </li>
                      <li class="text-[11px] text-amber-700 flex items-start gap-2">
                        <div class="w-1 h-1 rounded-full bg-amber-400 mt-1.5 shrink-0"></div>
                        增加 1 小时的自由活动时间，体验当地特色茶馆。
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <div class="lg:col-span-4 space-y-6">
                <!-- Real-time Status Card (New based on RealtimeStatusController) -->
                <div class="bg-white p-6 rounded-3xl border border-stone-200 shadow-sm space-y-6">
                  <div class="flex justify-between items-center">
                    <h3 class="text-sm font-bold text-stone-900 flex items-center gap-2">
                      <Clock class="w-4 h-4 text-emerald-600" /> 实时状态
                    </h3>
                    <span class="text-[10px] text-stone-400">更新于: {{ new Date(getRealtimeStatus(selectedRecommendation.id).lastUpdated).toLocaleTimeString() }}</span>
                  </div>

                  <div class="grid grid-cols-2 gap-4">
                    <div class="p-4 bg-stone-50 rounded-2xl border border-stone-100">
                      <div class="flex items-center gap-2 text-stone-400 mb-1">
                        <Users class="w-3 h-3" />
                        <span class="text-[9px] font-bold uppercase tracking-tighter">当前人流</span>
                      </div>
                      <div class="flex items-baseline gap-1">
                        <span class="text-lg font-bold text-stone-900">{{ getRealtimeStatus(selectedRecommendation.id).crowdCount }}</span>
                        <span class="text-[10px] text-stone-400">/ {{ getRealtimeStatus(selectedRecommendation.id).capacity }}</span>
                      </div>
                    </div>
                    <div class="p-4 bg-stone-50 rounded-2xl border border-stone-100">
                      <div class="flex items-center gap-2 text-stone-400 mb-1">
                        <Sparkles class="w-3 h-3" />
                        <span class="text-[9px] font-bold uppercase tracking-tighter">天气状况</span>
                      </div>
                      <div class="flex items-baseline gap-1">
                        <span class="text-lg font-bold text-stone-900">{{ getRealtimeStatus(selectedRecommendation.id).weather }}</span>
                        <span class="text-[10px] text-stone-400">{{ getRealtimeStatus(selectedRecommendation.id).temperature }}°C</span>
                      </div>
                    </div>
                  </div>

                  <div class="space-y-4">
                    <div class="space-y-2">
                      <div class="flex justify-between text-[10px] font-bold">
                        <span class="text-stone-400">人流拥挤度</span>
                        <span :class="getRealtimeStatus(selectedRecommendation.id).crowdCount > getRealtimeStatus(selectedRecommendation.id).capacity * 0.8 ? 'text-red-500' : 'text-emerald-600'">
                          {{ getRealtimeStatus(selectedRecommendation.id).crowdCount > getRealtimeStatus(selectedRecommendation.id).capacity * 0.8 ? '拥挤' : '舒适' }}
                        </span>
                      </div>
                      <div class="h-1.5 bg-stone-100 rounded-full overflow-hidden">
                        <div 
                          class="h-full transition-all duration-1000" 
                          :class="getRealtimeStatus(selectedRecommendation.id).crowdCount > getRealtimeStatus(selectedRecommendation.id).capacity * 0.8 ? 'bg-red-500' : 'bg-emerald-500'"
                          :style="{ width: (getRealtimeStatus(selectedRecommendation.id).crowdCount / getRealtimeStatus(selectedRecommendation.id).capacity * 100) + '%' }"
                        ></div>
                      </div>
                    </div>

                    <div class="p-3 bg-blue-50 rounded-xl border border-blue-100 flex items-start gap-3">
                      <ShieldAlert class="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                      <div>
                        <p class="text-[10px] font-bold text-blue-900">历史对比</p>
                        <p class="text-[9px] text-blue-700 mt-0.5">
                          当前人流比历史同期{{ getRealtimeStatus(selectedRecommendation.id).crowdCount > getRealtimeStatus(selectedRecommendation.id).historicalAvg ? '高' : '低' }} 
                          {{ Math.abs(Math.round((getRealtimeStatus(selectedRecommendation.id).crowdCount / getRealtimeStatus(selectedRecommendation.id).historicalAvg - 1) * 100)) }}%
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100 sticky top-0">
                  <div class="flex justify-between items-center mb-6">
                    <span class="text-stone-400 text-xs font-bold uppercase tracking-widest">起价</span>
                    <span class="text-2xl font-bold text-emerald-600">$1,499</span>
                  </div>
                  <div class="space-y-4 mb-8">
                    <div class="flex items-center justify-between text-sm">
                      <span class="text-stone-500">时长</span>
                      <span class="font-bold text-stone-900">7天</span>
                    </div>
                    <div class="flex items-center justify-between text-sm">
                      <span class="text-stone-500">团队人数</span>
                      <span class="font-bold text-stone-900">最多12人</span>
                    </div>
                    <div class="flex items-center justify-between text-sm">
                      <span class="text-stone-500">难度</span>
                      <span class="font-bold text-stone-900">中等</span>
                    </div>
                  </div>
                  <div class="space-y-3">
                    <button class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20">
                      立即预订
                    </button>
                    <button 
                      @click="toggleCompare(selectedRecommendation)"
                      :class="['w-full py-4 rounded-2xl font-bold border transition-all', routesToCompare.some(r => r.id === selectedRecommendation.id) ? 'bg-amber-50 border-amber-200 text-amber-600' : 'bg-white border-stone-200 text-stone-600 hover:bg-stone-50']"
                    >
                      {{ routesToCompare.some(r => r.id === selectedRecommendation.id) ? '已加入比较' : '加入路线比较' }}
                    </button>
                  </div>
                  <p class="text-center text-[10px] text-stone-400 mt-4">48小时前免费取消</p>
                </div>
              </div>
            </div>
          </div>
        </Motion>
      </div>
    </main>

    <!-- Minimal Footer -->
    <footer class="bg-white border-t border-stone-100 py-2 flex-shrink-0">
      <div class="max-w-7xl mx-auto px-4 flex justify-between items-center">
        <p class="text-[9px] text-stone-400">© 2026 智慧旅游系统 (Travel-API). 版权所有。</p>
        <div class="flex gap-4">
          <a href="#" class="text-stone-400 hover:text-emerald-600 transition-colors"><Camera class="w-3.5 h-3.5" /></a>
          <a href="#" class="text-stone-400 hover:text-emerald-600 transition-colors"><Compass class="w-3.5 h-3.5" /></a>
          <a href="#" class="text-stone-400 hover:text-emerald-600 transition-colors"><Plane class="w-3.5 h-3.5" /></a>
        </div>
      </div>
    </footer>
    <!-- AI Assistant Floating Button -->
    <div class="fixed bottom-6 right-6 z-[60]">
      <button 
        @click="showAIAssistant = !showAIAssistant"
        class="w-14 h-14 bg-emerald-600 rounded-full shadow-2xl flex items-center justify-center text-white hover:bg-emerald-700 transition-all hover:scale-110 active:scale-95"
      >
        <Sparkles v-if="!showAIAssistant" class="w-6 h-6" />
        <X v-else class="w-6 h-6" />
      </button>

      <Presence>
        <Motion
          v-if="showAIAssistant"
          :initial="{ opacity: 0, scale: 0.8, y: 20 }"
          :animate="{ opacity: 1, scale: 1, y: 0 }"
          :exit="{ opacity: 0, scale: 0.8, y: 20 }"
          class="absolute bottom-20 right-0 w-80 bg-white rounded-3xl shadow-2xl border border-stone-200 overflow-hidden flex flex-col"
        >
          <div class="bg-emerald-600 p-4 text-white">
            <div class="flex justify-between items-center mb-2">
              <h3 class="text-sm font-bold flex items-center gap-2">
                <Sparkles class="w-4 h-4" /> AI 旅行助手
              </h3>
              <div class="flex gap-1">
                <button 
                  v-for="mode in ['chat', 'translate', 'budget', 'safety']" 
                  :key="mode"
                  @click="aiMode = mode as any"
                  :class="['px-2 py-0.5 rounded text-[8px] font-bold uppercase transition-all', aiMode === mode ? 'bg-white text-emerald-600' : 'bg-emerald-500 text-white hover:bg-emerald-400']"
                >
                  {{ mode === 'chat' ? '对话' : mode === 'translate' ? '翻译' : mode === 'budget' ? '预算' : '安全' }}
                </button>
              </div>
            </div>
            <p class="text-[10px] opacity-80">
              {{ aiMode === 'chat' ? '问我任何关于你旅行的问题！' : aiMode === 'translate' ? '输入文字进行实时翻译' : aiMode === 'budget' ? '输入目的地获取预算估算' : '获取目的地的安全建议' }}
            </p>
          </div>
          <div class="flex-1 h-64 overflow-y-auto p-4 space-y-3 bg-stone-50">
            <div v-if="chatHistory.length === 0" class="text-center py-8">
              <Sparkles class="w-8 h-8 text-emerald-200 mx-auto mb-2" />
              <p class="text-xs text-stone-400">你好！我是你的 AI 助手。今天我能如何帮你规划旅程？</p>
            </div>
            <div 
              v-for="(msg, idx) in chatHistory" 
              :key="idx"
              :class="['max-w-[80%] p-2.5 rounded-2xl text-xs', msg.role === 'user' ? 'bg-emerald-600 text-white ml-auto rounded-tr-none' : 'bg-white text-stone-800 mr-auto rounded-tl-none shadow-sm']"
            >
              {{ msg.text }}
            </div>
          </div>
          <div class="p-3 bg-white border-t border-stone-100 flex flex-col gap-2">
            <div class="flex gap-2">
              <button 
                @click="analyzeImage"
                :disabled="isAnalyzingImage"
                class="p-2 bg-stone-100 rounded-xl text-stone-500 hover:text-emerald-600 transition-colors disabled:opacity-50"
                title="识别图片景点"
              >
                <Camera v-if="!isAnalyzingImage" class="w-4 h-4" />
                <div v-else class="w-4 h-4 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin"></div>
              </button>
              <input 
                v-model="aiMessage"
                @keyup.enter="sendAIMessage"
                type="text" 
                :placeholder="aiMode === 'chat' ? '输入消息...' : '输入内容...'" 
                class="flex-1 bg-stone-100 rounded-xl px-3 py-2 text-xs outline-none focus:ring-2 focus:ring-emerald-500/20"
              />
              <button 
                @click="sendAIMessage"
                class="bg-emerald-600 text-white p-2 rounded-xl hover:bg-emerald-700 transition-colors"
              >
                <ArrowRight class="w-4 h-4" />
              </button>
            </div>
          </div>
        </Motion>
      </Presence>
    </div>

    <!-- Comments Modal -->
    <Presence>
      <Motion
        v-if="showCommentsModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showCommentsModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-lg rounded-[32px] overflow-hidden shadow-2xl flex flex-col max-h-[80vh]"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-900">评论 ({{ selectedNoteForComments?.comments }})</h3>
            <button @click="showCommentsModal = false" class="w-8 h-8 rounded-full bg-stone-50 flex items-center justify-center text-stone-400 hover:text-stone-900 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="flex-1 overflow-y-auto p-6 space-y-6">
            <div v-for="comment in selectedNoteForComments?.commentList" :key="comment.id" class="flex gap-4">
              <div class="w-8 h-8 rounded-full bg-stone-100 flex-shrink-0 overflow-hidden">
                <img :src="`https://picsum.photos/seed/${comment.user}/100/100`" class="w-full h-full object-cover" />
              </div>
              <div>
                <p class="text-xs font-bold text-stone-900 mb-1">{{ comment.user }}</p>
                <p class="text-xs text-stone-600 leading-relaxed">{{ comment.text }}</p>
              </div>
            </div>
          </div>
          <div class="p-6 border-t border-stone-100 bg-stone-50">
            <div class="flex gap-3">
              <input 
                v-model="newCommentText"
                @keyup.enter="addComment"
                type="text" 
                placeholder="写下您的评论..." 
                class="flex-1 bg-white border border-stone-200 rounded-xl px-4 py-2 text-xs outline-none focus:border-emerald-600 transition-colors"
              />
              <button 
                @click="addComment"
                class="bg-emerald-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-emerald-700 transition-colors"
              >
                发送
              </button>
            </div>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Post Note Modal -->
    <Presence>
      <Motion
        v-if="showPostNoteModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showPostNoteModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-lg rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-900">发布旅行笔记</h3>
            <button @click="showPostNoteModal = false" class="w-8 h-8 rounded-full bg-stone-50 flex items-center justify-center text-stone-400 hover:text-stone-900 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="p-6 space-y-4">
            <div class="space-y-1.5">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">标题</label>
              <input 
                v-model="newNoteData.title"
                type="text" 
                placeholder="给你的笔记起个吸引人的标题..." 
                class="w-full bg-stone-50 border border-stone-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:border-emerald-600 transition-colors"
              />
            </div>
            <div class="space-y-1.5">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">内容</label>
              <textarea 
                v-model="newNoteData.excerpt"
                rows="4"
                placeholder="分享你的旅行故事、心得或攻略..." 
                class="w-full bg-stone-50 border border-stone-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:border-emerald-600 transition-colors resize-none"
              ></textarea>
            </div>
            <div class="space-y-1.5">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">封面图片链接 (可选)</label>
              <input 
                v-model="newNoteData.image"
                type="text" 
                placeholder="输入图片 URL..." 
                class="w-full bg-stone-50 border border-stone-200 rounded-xl px-4 py-2.5 text-sm outline-none focus:border-emerald-600 transition-colors"
              />
            </div>
          </div>
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex gap-3">
            <button 
              @click="showPostNoteModal = false"
              class="flex-1 px-6 py-3 rounded-xl text-sm font-bold text-stone-600 hover:bg-stone-100 transition-colors"
            >
              取消
            </button>
            <button 
              @click="postNote"
              :disabled="!newNoteData.title || !newNoteData.excerpt"
              class="flex-1 bg-emerald-600 text-white px-6 py-3 rounded-xl text-sm font-bold hover:bg-emerald-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              发布
            </button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Route Comparison Modal -->
    <Presence>
      <Motion
        v-if="showCompareModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showCompareModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-4xl rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-900">多维度路线比较</h3>
            <button @click="showCompareModal = false" class="w-8 h-8 rounded-full bg-stone-50 flex items-center justify-center text-stone-400 hover:text-stone-900 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="p-8 grid grid-cols-3 gap-8">
            <div v-for="route in routesToCompare" :key="route.id" class="space-y-6">
              <div class="aspect-video rounded-2xl overflow-hidden">
                <img :src="route.image" class="w-full h-full object-cover" />
              </div>
              <h4 class="font-bold text-stone-900 text-center">{{ route.title }}</h4>
              <div class="space-y-4">
                <div class="space-y-1">
                  <span class="text-[10px] font-bold text-stone-400 uppercase">质量评估</span>
                  <div class="h-2 bg-stone-100 rounded-full overflow-hidden">
                    <div class="h-full bg-emerald-500" :style="{ width: (85 + Math.random() * 10) + '%' }"></div>
                  </div>
                </div>
                <div class="space-y-1">
                  <span class="text-[10px] font-bold text-stone-400 uppercase">预算预估</span>
                  <p class="text-sm font-bold text-stone-700">¥{{ 1200 + Math.floor(Math.random() * 800) }}</p>
                </div>
                <div class="space-y-1">
                  <span class="text-[10px] font-bold text-stone-400 uppercase">景点丰富度</span>
                  <p class="text-sm font-bold text-stone-700">{{ 8 + Math.floor(Math.random() * 5) }} 个景点</p>
                </div>
              </div>
            </div>
          </div>
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex justify-end">
            <button @click="showCompareModal = false" class="bg-stone-900 text-white px-8 py-3 rounded-2xl text-sm font-bold">关闭比较</button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Real-time Adjustment Modal -->
    <Presence>
      <Motion
        v-if="showAdjustmentModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showAdjustmentModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-md rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-emerald-600 text-white">
            <h3 class="text-lg font-bold flex items-center gap-2">
              <Navigation class="w-5 h-5" /> 实时路线调整
            </h3>
            <button @click="showAdjustmentModal = false" class="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center text-white hover:bg-white/40 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="p-8 space-y-6">
            <div class="p-4 bg-amber-50 rounded-2xl border border-amber-100">
              <p class="text-xs text-amber-800 font-medium leading-relaxed">
                检测到目的地附近人流量激增，建议调整下午的游览顺序以获得更好体验。
              </p>
            </div>
            <div class="space-y-4">
              <h4 class="text-xs font-bold text-stone-400 uppercase tracking-widest">调整方案</h4>
              <div class="space-y-3">
                <div class="flex items-center gap-3 p-3 bg-stone-50 rounded-xl border border-stone-100">
                  <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center text-[10px] font-bold">1</div>
                  <span class="text-xs text-stone-700">提前前往“清水寺”以避开高峰</span>
                </div>
                <div class="flex items-center gap-3 p-3 bg-stone-50 rounded-xl border border-stone-100">
                  <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center text-[10px] font-bold">2</div>
                  <span class="text-xs text-stone-700">将“传统茶道体验”延后至 16:00</span>
                </div>
              </div>
            </div>
          </div>
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex gap-3">
            <button @click="showAdjustmentModal = false" class="flex-1 py-3 rounded-xl text-sm font-bold text-stone-600 hover:bg-stone-100 transition-colors">忽略</button>
            <button @click="showAdjustmentModal = false" class="flex-1 bg-emerald-600 text-white py-3 rounded-xl text-sm font-bold hover:bg-emerald-700 transition-colors">应用调整</button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Route Comparison Modal -->
    <Presence>
      <Motion
        v-if="showCompareModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showCompareModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-4xl rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-900">多维度路线比较</h3>
            <button @click="showCompareModal = false" class="w-8 h-8 rounded-full bg-stone-50 flex items-center justify-center text-stone-400 hover:text-stone-900 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="p-8 grid grid-cols-3 gap-8">
            <div v-for="route in routesToCompare" :key="route.id" class="space-y-6">
              <div class="aspect-video rounded-2xl overflow-hidden">
                <img :src="route.image" class="w-full h-full object-cover" />
              </div>
              <h4 class="font-bold text-stone-900 text-center">{{ route.title }}</h4>
              <div class="space-y-4">
                <div class="space-y-1">
                  <span class="text-[10px] font-bold text-stone-400 uppercase">质量评估</span>
                  <div class="h-2 bg-stone-100 rounded-full overflow-hidden">
                    <div class="h-full bg-emerald-500" :style="{ width: (85 + Math.random() * 10) + '%' }"></div>
                  </div>
                </div>
                <div class="space-y-1">
                  <span class="text-[10px] font-bold text-stone-400 uppercase">预算预估</span>
                  <p class="text-sm font-bold text-stone-700">¥{{ 1200 + Math.floor(Math.random() * 800) }}</p>
                </div>
                <div class="space-y-1">
                  <span class="text-[10px] font-bold text-stone-400 uppercase">景点丰富度</span>
                  <p class="text-sm font-bold text-stone-700">{{ 8 + Math.floor(Math.random() * 5) }} 个景点</p>
                </div>
              </div>
            </div>
          </div>
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex justify-end">
            <button @click="showCompareModal = false" class="bg-stone-900 text-white px-8 py-3 rounded-2xl text-sm font-bold">关闭比较</button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Real-time Adjustment Modal -->
    <Presence>
      <Motion
        v-if="showAdjustmentModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showAdjustmentModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-md rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-emerald-600 text-white">
            <h3 class="text-lg font-bold flex items-center gap-2">
              <Navigation class="w-5 h-5" /> 实时路线调整
            </h3>
            <button @click="showAdjustmentModal = false" class="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center text-white hover:bg-white/40 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="p-8 space-y-6">
            <div class="p-4 bg-amber-50 rounded-2xl border border-amber-100">
              <p class="text-xs text-amber-800 font-medium leading-relaxed">
                检测到目的地附近人流量激增，建议调整下午的游览顺序以获得更好体验。
              </p>
            </div>
            <div class="space-y-4">
              <h4 class="text-xs font-bold text-stone-400 uppercase tracking-widest">调整方案</h4>
              <div class="space-y-3">
                <div class="flex items-center gap-3 p-3 bg-stone-50 rounded-xl border border-stone-100">
                  <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center text-[10px] font-bold">1</div>
                  <span class="text-xs text-stone-700">提前前往“清水寺”以避开高峰</span>
                </div>
                <div class="flex items-center gap-3 p-3 bg-stone-50 rounded-xl border border-stone-100">
                  <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center text-[10px] font-bold">2</div>
                  <span class="text-xs text-stone-700">将“传统茶道体验”延后至 16:00</span>
                </div>
              </div>
            </div>
          </div>
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex gap-3">
            <button @click="showAdjustmentModal = false" class="flex-1 py-3 rounded-xl text-sm font-bold text-stone-600 hover:bg-stone-100 transition-colors">忽略</button>
            <button @click="showAdjustmentModal = false" class="flex-1 bg-emerald-600 text-white py-3 rounded-xl text-sm font-bold hover:bg-emerald-700 transition-colors">应用调整</button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Collaboration Modal -->
    <Presence>
      <Motion
        v-if="showCollaborationModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showCollaborationModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-2xl rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-stone-900 text-white">
            <div class="flex items-center gap-3">
              <Users2 class="w-5 h-5 text-emerald-400" />
              <div>
                <h3 class="text-lg font-bold">多人协作规划</h3>
                <p class="text-[10px] text-stone-400 uppercase tracking-widest font-bold">正在协作: {{ activeTripForCollaboration?.title }}</p>
              </div>
            </div>
            <button @click="showCollaborationModal = false" class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white hover:bg-white/20 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          
          <div class="flex-1 overflow-y-auto p-8 grid grid-cols-1 md:grid-cols-2 gap-8">
            <!-- Collaborators List -->
            <div class="space-y-6">
              <div class="flex justify-between items-center">
                <h4 class="text-xs font-bold text-stone-400 uppercase tracking-widest">协作者 ({{ collaborators.length }})</h4>
                <button class="text-emerald-600 hover:text-emerald-700 transition-colors">
                  <UserPlus class="w-4 h-4" />
                </button>
              </div>
              <div class="space-y-4">
                <div v-for="user in collaborators" :key="user.id" class="flex items-center justify-between p-3 bg-stone-50 rounded-2xl border border-stone-100">
                  <div class="flex items-center gap-3">
                    <div class="relative">
                      <img :src="user.avatar" class="w-10 h-10 rounded-full object-cover" />
                      <div v-if="user.isOnline" class="absolute bottom-0 right-0 w-3 h-3 bg-emerald-500 border-2 border-white rounded-full"></div>
                    </div>
                    <div>
                      <p class="text-sm font-bold text-stone-900">{{ user.name }}</p>
                      <p class="text-[10px] text-stone-400 font-bold uppercase">{{ user.role === 'owner' ? '所有者' : user.role === 'editor' ? '编辑者' : '查看者' }}</p>
                    </div>
                  </div>
                  <div class="flex gap-1">
                    <button v-if="user.role !== 'owner'" class="p-1.5 text-stone-400 hover:text-stone-900 transition-colors">
                      <Edit3 class="w-3.5 h-3.5" />
                    </button>
                    <button v-if="user.role !== 'owner'" class="p-1.5 text-stone-400 hover:text-red-500 transition-colors">
                      <Trash2 class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Task List -->
            <div class="space-y-6">
              <h4 class="text-xs font-bold text-stone-400 uppercase tracking-widest">任务清单</h4>
              <div class="space-y-3">
                <div v-for="task in tripTasks" :key="task.id" class="flex items-center gap-3 p-4 bg-stone-50 rounded-2xl border border-stone-100 group">
                  <button @click="toggleTask(task)" class="w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all" :class="task.isCompleted ? 'bg-emerald-500 border-emerald-500 text-white' : 'border-stone-200 text-transparent group-hover:border-emerald-500'">
                    <CheckCircle2 class="w-3.5 h-3.5" />
                  </button>
                  <span class="text-xs font-medium" :class="task.isCompleted ? 'text-stone-400 line-through' : 'text-stone-700'">{{ task.description }}</span>
                </div>
                <button class="w-full py-3 border-2 border-dashed border-stone-200 rounded-2xl text-stone-400 text-xs font-bold hover:border-emerald-500 hover:text-emerald-600 transition-all">
                  + 添加新任务
                </button>
              </div>
            </div>
          </div>
          
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex justify-end">
            <button @click="showCollaborationModal = false" class="bg-stone-900 text-white px-8 py-3 rounded-2xl text-sm font-bold hover:bg-stone-800 transition-all">完成设置</button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Analytics Modal -->
    <Presence>
      <Motion
        v-if="showAnalyticsModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showAnalyticsModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-4xl rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-emerald-600 text-white">
            <div class="flex items-center gap-3">
              <BarChart3 class="w-5 h-5" />
              <h3 class="text-lg font-bold">平台旅行大数据概览</h3>
            </div>
            <button @click="showAnalyticsModal = false" class="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center text-white hover:bg-white/40 transition-colors">
              <X class="w-4 h-4" />
            </button>
          </div>
          
          <div class="p-8 grid grid-cols-1 md:grid-cols-4 gap-6">
            <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100">
              <p class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mb-2">总用户数</p>
              <div class="flex items-end gap-2">
                <span class="text-2xl font-bold text-stone-900">{{ platformOverview.totalUsers }}</span>
                <span class="text-[10px] text-emerald-600 font-bold mb-1">+12%</span>
              </div>
            </div>
            <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100">
              <p class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mb-2">活跃路线</p>
              <div class="flex items-end gap-2">
                <span class="text-2xl font-bold text-stone-900">{{ platformOverview.activeRoutes }}</span>
                <span class="text-[10px] text-emerald-600 font-bold mb-1">+5%</span>
              </div>
            </div>
            <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100">
              <p class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mb-2">完成行程</p>
              <div class="flex items-end gap-2">
                <span class="text-2xl font-bold text-stone-900">{{ platformOverview.completedTrips }}</span>
                <span class="text-[10px] text-emerald-600 font-bold mb-1">+18%</span>
              </div>
            </div>
            <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100">
              <p class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mb-2">平均满意度</p>
              <div class="flex items-center gap-2">
                <span class="text-2xl font-bold text-stone-900">{{ platformOverview.avgSatisfaction }}</span>
                <div class="flex text-amber-400">
                  <Star v-for="i in 5" :key="i" class="w-3 h-3 fill-current" />
                </div>
              </div>
            </div>
          </div>

          <div class="px-8 pb-8 grid grid-cols-1 md:grid-cols-2 gap-8">
            <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100">
              <h4 class="text-xs font-bold text-stone-900 mb-6 flex items-center gap-2">
                <TrendingUp class="w-4 h-4 text-emerald-600" /> 热门目的地趋势
              </h4>
              <div class="h-48 flex items-end gap-4">
                <div v-for="(h, i) in [60, 85, 45, 90, 70, 55]" :key="i" class="flex-1 bg-emerald-100 rounded-t-lg relative group transition-all hover:bg-emerald-500">
                  <div class="absolute -top-6 left-1/2 -translate-x-1/2 text-[10px] font-bold text-stone-400 opacity-0 group-hover:opacity-100 transition-opacity">{{ h }}%</div>
                  <div class="w-full bg-emerald-500 rounded-t-lg transition-all duration-1000" :style="{ height: h + '%' }"></div>
                </div>
              </div>
              <div class="flex justify-between mt-4 text-[10px] font-bold text-stone-400 uppercase tracking-tighter">
                <span>京都</span><span>巴黎</span><span>巴厘岛</span><span>伦敦</span><span>纽约</span><span>东京</span>
              </div>
            </div>
            <div class="bg-stone-50 p-6 rounded-3xl border border-stone-100">
              <h4 class="text-xs font-bold text-stone-900 mb-6 flex items-center gap-2">
                <PieChart class="w-4 h-4 text-emerald-600" /> 旅行方式分布
              </h4>
              <div class="flex items-center justify-around h-48">
                <div class="relative w-32 h-32 rounded-full border-[12px] border-emerald-500 flex items-center justify-center">
                  <div class="absolute inset-0 rounded-full border-[12px] border-emerald-100 border-t-transparent border-r-transparent rotate-45"></div>
                  <span class="text-xl font-bold text-stone-900">75%</span>
                </div>
                <div class="space-y-3">
                  <div class="flex items-center gap-2">
                    <div class="w-3 h-3 rounded-full bg-emerald-500"></div>
                    <span class="text-[10px] font-bold text-stone-600">自由行</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <div class="w-3 h-3 rounded-full bg-emerald-100"></div>
                    <span class="text-[10px] font-bold text-stone-600">跟团游</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <div class="w-3 h-3 rounded-full bg-stone-200"></div>
                    <span class="text-[10px] font-bold text-stone-600">定制游</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Auth Modal (based on UserController) -->
    <Presence>
      <Motion
        v-if="showAuthModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[120] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showAuthModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-sm rounded-[32px] overflow-hidden shadow-2xl p-8"
        >
          <div class="text-center space-y-2 mb-8">
            <h3 class="text-2xl font-serif font-bold text-stone-900">{{ authMode === 'login' ? '欢迎回来' : '加入我们' }}</h3>
            <p class="text-xs text-stone-500">{{ authMode === 'login' ? '开启您的下一段智慧旅程' : '创建账号，探索更多精彩行程' }}</p>
          </div>

          <div v-if="authMode === 'login'" class="space-y-4">
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">用户名</label>
              <input v-model="loginForm.username" type="text" placeholder="输入用户名" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">密码</label>
              <input v-model="loginForm.password" type="password" placeholder="输入密码" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <button @click="handleLogin" class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20">登录系统</button>
          </div>

          <div v-else class="space-y-4">
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">用户名</label>
              <input v-model="registerForm.username" type="text" placeholder="起个好听的名字" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">密码</label>
              <input v-model="registerForm.password" type="password" placeholder="设置安全密码" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <div class="flex items-start gap-2">
              <input v-model="registerForm.agreement" type="checkbox" class="mt-1" />
              <label class="text-[10px] text-stone-500">我已阅读并同意《用户协议》和《隐私政策》</label>
            </div>
            <button @click="handleRegister" class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20">立即注册</button>
          </div>

          <div class="mt-8 pt-6 border-t border-stone-100 text-center">
            <button @click="authMode = (authMode === 'login' ? 'register' : 'login')" class="text-xs font-bold text-emerald-600 hover:text-emerald-700">
              {{ authMode === 'login' ? '还没有账号？立即注册' : '已有账号？返回登录' }}
            </button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Comments Modal (based on RouteCommentController) -->
    <Presence>
      <Motion
        v-if="showCommentsModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[110] flex items-center justify-end bg-stone-900/40 backdrop-blur-sm"
        @click.self="showCommentsModal = false"
      >
        <Motion
          :initial="{ x: '100%' }"
          :animate="{ x: 0 }"
          :exit="{ x: '100%' }"
          :transition="{ duration: 0.5, easing: [0.22, 1, 0.36, 1] }"
          class="bg-white w-full max-w-lg h-full shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-white sticky top-0 z-10">
            <h3 class="text-lg font-bold flex items-center gap-2">
              <MessageSquare class="w-5 h-5 text-emerald-600" />
              评论 ({{ selectedNoteForComments?.comments }})
            </h3>
            <button @click="showCommentsModal = false" class="p-2 hover:bg-stone-100 rounded-full transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="flex-1 overflow-y-auto p-6 space-y-8 custom-scrollbar">
            <div v-for="comment in selectedNoteForComments?.commentList" :key="comment.id" class="space-y-4">
              <div class="flex gap-4">
                <div class="w-10 h-10 rounded-full bg-stone-200 shrink-0">
                  <img :src="`https://picsum.photos/seed/${comment.user}/100/100`" class="w-full h-full object-cover rounded-full" />
                </div>
                <div class="flex-1">
                  <div class="bg-stone-50 p-4 rounded-2xl border border-stone-100 relative group">
                    <p class="text-[10px] font-bold text-stone-900 mb-1 uppercase tracking-tight">{{ comment.user }}</p>
                    <p class="text-xs text-stone-700 leading-relaxed">{{ comment.text }}</p>
                    <div class="absolute -bottom-3 right-4 flex gap-2">
                      <button @click="setReply(comment.id, comment.user)" class="bg-white px-2 py-1 rounded-full border border-stone-100 shadow-sm text-[9px] font-bold text-stone-400 hover:text-emerald-600 transition-all uppercase tracking-widest">回复</button>
                    </div>
                  </div>
                  
                  <!-- Replies -->
                  <div v-if="comment.replies?.length" class="mt-4 ml-8 space-y-3">
                    <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-3">
                      <div class="w-8 h-8 rounded-full bg-stone-100 shrink-0">
                        <img :src="`https://picsum.photos/seed/${reply.user}/100/100`" class="w-full h-full object-cover rounded-full" />
                      </div>
                      <div class="bg-stone-50/50 p-3 rounded-xl border border-stone-100 flex-1">
                        <p class="text-[9px] font-bold text-stone-900 mb-0.5">{{ reply.user }}</p>
                        <p class="text-[10px] text-stone-600">{{ reply.text }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="!selectedNoteForComments?.commentList?.length" class="text-center py-20">
              <div class="w-16 h-16 bg-stone-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <MessageSquare class="w-8 h-8 text-stone-200" />
              </div>
              <p class="text-xs text-stone-400">目前还没有评论，快来抢沙发吧！</p>
            </div>
          </div>

          <div class="p-6 border-t border-stone-100 bg-white sticky bottom-0">
            <div class="flex items-center gap-3">
              <input 
                v-model="newCommentText" 
                type="text" 
                :placeholder="replyUsername ? `正在回复 @${replyUsername}...` : '写下您的想法...'" 
                class="flex-1 bg-stone-50 border border-stone-100 rounded-2xl px-5 py-3 text-sm outline-none focus:border-emerald-600 transition-all"
                @keyup.enter="submitComment"
              />
              <button @click="submitComment" class="bg-emerald-600 text-white w-12 h-12 rounded-2xl flex items-center justify-center hover:bg-emerald-700 transition-all shadow-md">
                <ArrowUp class="w-5 h-5" />
              </button>
            </div>
            <p v-if="replyUsername" class="text-[9px] text-emerald-600 font-bold uppercase tracking-widest mt-2 ml-4">
              点击输入框外可取消回复
            </p>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Share Modal (based on RouteShareController and FileShareController) -->
    <Presence>
      <Motion
        v-if="showShareModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[120] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showShareModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-sm rounded-[32px] overflow-hidden shadow-2xl p-8 text-center"
        >
          <div class="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-6">
            <Share2 class="w-8 h-8" />
          </div>
          <h3 class="text-xl font-bold text-stone-900 mb-2">生成分享链接</h3>
          <p class="text-xs text-stone-500 mb-8 lowercase tracking-tight">正在为 “{{ activeItemForSharing?.title || activeItemForSharing?.fileName }}” 创建私密分享</p>
          
          <div v-if="isGeneratingShareCode" class="flex flex-col items-center gap-4 py-6">
            <div class="w-8 h-8 border-4 border-emerald-600 border-t-transparent rounded-full animate-spin"></div>
            <p class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">正在联系服务器...</p>
          </div>
          <div v-else-if="shareCodeResponse" class="space-y-6">
            <div class="bg-stone-50 p-6 rounded-2xl border border-dashed border-stone-200">
              <p class="text-[10px] font-bold text-stone-400 uppercase tracking-widest mb-2">唯一分享码</p>
              <p class="text-3xl font-mono font-bold text-emerald-600 tracking-wider">{{ shareCodeResponse }}</p>
            </div>
            <p class="text-[10px] text-stone-400 px-4">分享码有效期为 7 天。您可以开启密码保护以提高安全性。</p>
            <div class="flex gap-3">
              <button @click="showShareModal = false" class="flex-1 py-3 rounded-xl text-xs font-bold text-stone-600 bg-stone-100 hover:bg-stone-200">关闭</button>
              <button @click="handleCopyLink" class="flex-1 bg-emerald-600 text-white py-3 rounded-xl text-xs font-bold hover:bg-emerald-700">复制链接</button>
            </div>
          </div>
        </Motion>
      </Motion>
    </Presence>
    <!-- Validate Share Code Modal -->
    <Presence>
      <Motion
          v-if="showValidateShareModal"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: 1 }"
          :exit="{ opacity: 0 }"
          class="fixed inset-0 z-[120] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
          @click.self="showValidateShareModal = false"
      >
        <Motion
            :initial="{ scale: 0.9, opacity: 0, y: 20 }"
            :animate="{ scale: 1, opacity: 1, y: 0 }"
            class="bg-white w-full max-w-sm rounded-[32px] overflow-hidden shadow-2xl p-8"
        >
          <div class="text-center space-y-2 mb-8">
            <h3 class="text-2xl font-serif font-bold text-stone-900">验证分享码</h3>
            <p class="text-xs text-stone-500">输入分享码查看内容</p>
          </div>

          <div class="space-y-4">
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">分享码</label>
              <input v-model="validateShareCode" type="text" placeholder="输入分享码" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all font-mono tracking-wider" />
            </div>
            <button @click="handleValidateShareCode" class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20">验证并查看</button>
          </div>

          <div v-if="shareInfo" class="mt-6 pt-6 border-t border-stone-100">
            <div class="bg-emerald-50 p-4 rounded-xl border border-emerald-100">
              <p class="text-xs font-bold text-emerald-900 mb-1">分享信息</p>
              <p class="text-[10px] text-emerald-700">类型: {{ shareInfo.itemType }}</p>
              <p class="text-[10px] text-emerald-700">ID: {{ shareInfo.itemId }}</p>
              <p class="text-[10px] text-emerald-700">过期时间: {{ shareInfo.expireTime }}</p>
            </div>
          </div>

          <div class="mt-6 pt-6 border-t border-stone-100 text-center">
            <button @click="showValidateShareModal = false" class="text-xs font-bold text-stone-400 hover:text-stone-600">关闭</button>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Change Password Modal -->
    <Presence>
      <Motion
          v-if="showChangePasswordModal"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: 1 }"
          :exit="{ opacity: 0 }"
          class="fixed inset-0 z-[120] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
          @click.self="showChangePasswordModal = false"
      >
        <Motion
            :initial="{ scale: 0.9, opacity: 0, y: 20 }"
            :animate="{ scale: 1, opacity: 1, y: 0 }"
            class="bg-white w-full max-w-sm rounded-[32px] overflow-hidden shadow-2xl p-8"
        >
          <div class="text-center space-y-2 mb-8">
            <div class="w-16 h-16 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Lock class="w-8 h-8" />
            </div>
            <h3 class="text-2xl font-serif font-bold text-stone-900">修改密码</h3>
            <p class="text-xs text-stone-500">为了账户安全，请定期更换密码</p>
          </div>

          <div class="space-y-4">
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">原密码</label>
              <input v-model="passwordForm.oldPassword" type="password" placeholder="输入原密码" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">新密码</label>
              <input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">确认新密码</label>
              <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
            </div>
            <button @click="handleChangePassword" class="w-full bg-emerald-600 text-white py-4 rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-600/20">确认修改</button>
          </div>

          <div class="mt-6 pt-6 border-t border-stone-100 text-center">
            <button @click="showChangePasswordModal = false" class="text-xs font-bold text-stone-400 hover:text-stone-600">取消</button>
          </div>
        </Motion>
      </Motion>
    </Presence>


    <!-- Route Optimization Modal -->
    <Presence>
      <Motion
        v-if="showOptimizationHistory"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showOptimizationHistory = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-md rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-stone-900 text-white">
            <div class="flex items-center gap-3">
              <Sparkles class="w-5 h-5 text-emerald-400" />
              <h3 class="text-lg font-bold">AI 路线优化建议</h3>
            </div>
            <button @click="showOptimizationHistory = false" class="p-2 hover:bg-white/10 rounded-full transition-colors text-white">
              <X class="w-5 h-5" />
            </button>
          </div>
          <div class="p-8 space-y-6">
            <div v-for="suggestion in optimizationSuggestions" :key="suggestion.id" class="p-4 bg-stone-50 rounded-2xl border border-stone-100 space-y-3">
              <div class="flex justify-between items-start">
                <div class="flex items-center gap-2">
                  <div :class="[
                    'p-1.5 rounded-lg',
                    suggestion.type === 'time' ? 'bg-blue-50 text-blue-600' :
                    suggestion.type === 'cost' ? 'bg-emerald-50 text-emerald-600' :
                    'bg-amber-50 text-amber-600'
                  ]">
                    <Clock v-if="suggestion.type === 'time'" class="w-3.5 h-3.5" />
                    <CircleDollarSign v-else-if="suggestion.type === 'cost'" class="w-3.5 h-3.5" />
                    <Navigation v-else class="w-3.5 h-3.5" />
                  </div>
                  <span class="text-sm font-bold text-stone-900">{{ suggestion.title }}</span>
                </div>
                <span class="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">{{ suggestion.impact }}</span>
              </div>
              <p class="text-xs text-stone-500 leading-relaxed">{{ suggestion.description }}</p>
              <button @click="applyOptimization(suggestion)" class="w-full py-2 bg-stone-900 text-white rounded-xl text-[10px] font-bold hover:bg-stone-800 transition-all">应用此建议</button>
            </div>
          </div>
        </Motion>
      </Motion>
    </Presence>

    <!-- Post Note Modal (based on TravelNoteController) -->
    <Presence>
      <Motion
        v-if="showPostNoteModal"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        class="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
        @click.self="showPostNoteModal = false"
      >
        <Motion
          :initial="{ scale: 0.9, opacity: 0, y: 20 }"
          :animate="{ scale: 1, opacity: 1, y: 0 }"
          class="bg-white w-full max-w-xl rounded-[32px] overflow-hidden shadow-2xl flex flex-col"
        >
          <div class="p-6 border-b border-stone-100 flex justify-between items-center bg-stone-900 text-white">
            <h3 class="text-lg font-bold flex items-center gap-2">
              <Camera class="w-5 h-5 text-emerald-400" />
              分享您的旅行精彩
            </h3>
            <button @click="showPostNoteModal = false" class="p-2 hover:bg-white/10 rounded-full transition-colors text-white">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <div class="p-8 space-y-6">
            <div class="space-y-4">
              <div class="space-y-1">
                <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">笔记标题</label>
                <input v-model="newNoteData.title" type="text" placeholder="给您的分享起一个吸引人的标题..." class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
              </div>
              <div class="space-y-1">
                <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">分享内容</label>
                <textarea v-model="newNoteData.excerpt" rows="4" placeholder="讲述您的旅行故事，分享实用的小贴士..." class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all resize-none"></textarea>
              </div>
              <div class="space-y-1">
                <label class="text-[10px] font-bold text-stone-400 uppercase tracking-widest">封面链接 (可选)</label>
                <input v-model="newNoteData.image" type="text" placeholder="https://..." class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-600 transition-all" />
              </div>
            </div>
            
            <div class="p-6 bg-emerald-50 rounded-2xl border border-emerald-100 space-y-3">
              <div class="flex items-center gap-2">
                <Sparkles class="w-4 h-4 text-emerald-600" />
                <span class="text-xs font-bold text-emerald-700">AI 智能辅助已开启</span>
              </div>
              <p class="text-[10px] text-emerald-600/80 leading-relaxed font-medium">
                AI 将为您生成优美的摘要，并自动分析笔记的情感趋势，为社区用户提供更好的阅读体验。
              </p>
            </div>
          </div>
          
          <div class="p-6 border-t border-stone-100 bg-stone-50 flex gap-4">
            <button @click="showPostNoteModal = false" class="flex-1 py-4 rounded-2xl text-sm font-bold text-stone-600 hover:bg-stone-100 transition-all">存为草稿</button>
            <button @click="postNote" class="flex-1 bg-emerald-600 text-white py-4 rounded-2xl text-sm font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-500/20">立即发布</button>
          </div>
        </Motion>
      </Motion>
    </Presence>
  </div>
</template>

<style scoped>
/* Scoped styles if needed */
</style>
