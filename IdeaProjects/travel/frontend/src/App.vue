<template>
  <div class="min-h-screen flex flex-col bg-[radial-gradient(circle_at_top,_rgba(245,158,11,0.14),_transparent_28%),linear-gradient(180deg,#fafaf9_0%,#f5f5f4_100%)] text-stone-900">
    <div class="pointer-events-none fixed inset-x-0 top-0 z-0 h-72 bg-[radial-gradient(circle_at_top_right,_rgba(14,165,233,0.12),_transparent_30%),radial-gradient(circle_at_top_left,_rgba(245,158,11,0.18),_transparent_32%)]"></div>
    <AppHeader />
    <main class="relative z-10 flex-1 pt-20 md:pt-24">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import AppHeader from './components/layout/AppHeader.vue'
import AppFooter from './components/layout/AppFooter.vue'
</script>
