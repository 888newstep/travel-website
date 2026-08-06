from pathlib import Path

root = Path(r'C:\Users\xiaohongfu\IdeaProjects\travel\frontend')

content = (root / 'src' / 'pages' / 'HomePage.tsx').read_text(encoding='utf-8')

# Replace stone colors with slate/sky/emerald
content = content.replace('text-stone-900', 'text-slate-900')
content = content.replace('text-stone-600', 'text-slate-600')
content = content.replace('text-stone-500', 'text-slate-500')
content = content.replace('text-stone-400', 'text-slate-400')
content = content.replace('text-stone-700', 'text-slate-700')
content = content.replace('bg-stone-50', 'bg-sky-50')
content = content.replace('bg-stone-100', 'bg-sky-100')
content = content.replace('hover:bg-stone-100', 'hover:bg-sky-50')
content = content.replace('hover:text-stone-900', 'hover:text-sky-700')

# Update hero section gradient
content = content.replace(
    'bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_45%,rgba(240,249,255,0.9))]',
    'bg-gradient-to-br from-white via-sky-50/50 to-emerald-50/50'
)

# Update placeholder gradient
content = content.replace(
    'bg-[linear-gradient(135deg,rgba(245,158,11,0.14),rgba(14,165,233,0.14))]',
    'bg-gradient-to-br from-sky-100 to-emerald-100'
)

# Update chip text
content = content.replace('>React baseline<', '>智慧旅游<')
content = content.replace('>保留现有 API 层<', '>智能推荐<')
content = content.replace('>渐进迁移页面<', '>一站式服务<')

# Update hero title
content = content.replace(
    '旅行前端正在切换到 React 架构',
    '探索世界，智在掌握'
)

# Update hero description
content = content.replace(
    '当前版本已经把应用入口、布局、路由和首页切到 React，同时继续复用现有接口和样式体系，方便后续按批次迁移业务页面。',
    '为您提供智能景点推荐、路线规划、实时动态、AI 助手等一站式旅游服务，让每一次出行都充满智慧与乐趣。'
)

# Update section descriptions
content = content.replace(
    '继续复用原来的景点推荐接口，先让 React 首页具备真实数据。',
    '精选热门景点，为您推荐最佳旅游目的地。'
)
content = content.replace(
    '路线页将在后续迁移，此处先保留真实数据预览。',
    '智能路线规划，让行程更加轻松愉快。'
)
content = content.replace(
    '社区内容稍后会迁移成独立 React 业务模块。',
    '分享您的旅行故事，记录美好时光。'
)

# Update quick entries descriptions
content = content.replace(
    "description: '浏览热门景点与目的地内容'",
    "description: '发现精彩目的地'"
)
content = content.replace(
    "description: '查看推荐路线并继续完善行程'",
    "description: '智能规划您的行程'"
)
content = content.replace(
    "description: '快速了解景点开放与拥挤情况'",
    "description: '实时掌握景点动态'"
)
content = content.replace(
    "description: '获取个性化建议与行程灵感'",
    "description: 'AI 为您定制旅行方案'"
)

# Add animation classes to main sections
content = content.replace(
    '<section className="surface-card overflow-hidden rounded-[2rem]',
    '<section className="surface-card overflow-hidden rounded-[2rem] animate-slide-up'
)

# Add stagger animation to quick entries
content = content.replace(
    '<Link key={item.to} to={item.to} className="rounded-2xl',
    '<Link key={item.to} to={item.to} className="rounded-2xl animate-scale-in'
)

# Add hover animation to attraction cards
content = content.replace(
    '<article key={item.id ?? `${item.name}-${index}`} className="surface-card surface-card-hover',
    '<article key={item.id ?? `${item.name}-${index}`} className="surface-card surface-card-hover animate-fade-in'
)

# Update footer text
content = content.replace(
    "暂无推荐数据",
    "暂无推荐景点"
)

(root / 'src' / 'pages' / 'HomePage.tsx').write_text(content, encoding='utf-8', newline='\n')
print('Updated src/pages/HomePage.tsx')