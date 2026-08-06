from pathlib import Path

root = Path(r'C:\Users\xiaohongfu\IdeaProjects\travel\frontend')

# 1. Update src/index.css
css_content = r"""@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Playfair+Display:ital,wght@0,400;0,700;1,400&display=swap');
@import "tailwindcss";

@theme {
  --font-sans: "Inter", ui-sans-serif, system-ui, sans-serif;
  --font-serif: "Playfair Display", serif;
}

@layer base {
  html {
    scroll-behavior: smooth;
  }

  body {
    @apply font-sans text-slate-900 bg-gradient-to-br from-sky-50 via-white to-emerald-50 antialiased;
  }

  ::selection {
    @apply bg-sky-200 text-slate-900;
  }
}

@layer components {
  .app-container {
    @apply max-w-6xl mx-auto px-4 sm:px-6 lg:px-8;
  }

  .glass-panel {
    @apply bg-white/85 backdrop-blur-xl border border-white/80 shadow-[0_20px_60px_-30px_rgba(14,165,233,0.25)];
  }

  .surface-card {
    @apply bg-white/90 backdrop-blur-sm border border-slate-200/60 rounded-3xl shadow-[0_18px_45px_-30px_rgba(14,165,233,0.2)] transition-all duration-300;
  }

  .surface-card-hover {
    @apply hover:-translate-y-1 hover:shadow-[0_22px_50px_-28px_rgba(14,165,233,0.3)] hover:border-sky-300/60;
  }

  .section-heading {
    @apply text-2xl md:text-3xl font-semibold tracking-tight text-slate-900;
  }

  .section-subtitle {
    @apply text-sm md:text-base text-slate-500;
  }

  .chip {
    @apply inline-flex items-center gap-2 rounded-full border border-sky-200/80 bg-white/80 px-3 py-1 text-xs font-medium text-sky-700 shadow-sm;
  }
}

@layer utilities {
  .scrollbar-hide::-webkit-scrollbar {
    display: none;
  }

  .scrollbar-hide {
    -ms-overflow-style: none;
    scrollbar-width: none;
  }

  .animate-fade-in {
    animation: fadeIn 0.5s ease-out;
  }

  .animate-slide-up {
    animation: slideUp 0.6s ease-out;
  }

  .animate-scale-in {
    animation: scaleIn 0.4s ease-out;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
"""

(root / 'src' / 'index.css').write_text(css_content, encoding='utf-8', newline='\n')
print('Updated src/index.css')