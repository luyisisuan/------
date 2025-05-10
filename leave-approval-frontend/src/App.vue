<script setup>
import Navbar from './components/layout/Navbar.vue';
// import FooterComponent from './components/layout/FooterComponent.vue';
</script>

<template>
  <div id="app-layout">
    <Navbar />
    <main class="main-content">
      <div class="main-content-inner-container">
        <router-view v-slot="{ Component, route }">
          <transition name="page-transition" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </div>
    </main>
    {/* <FooterComponent /> */}
  </div>
</template>

<style scoped>
#app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--bg-primary, #f0f4f8); /* 使用全局变量，并提供一个备用颜色 */
  transition: background-color var(--transition-base); /* 平滑背景色过渡 */
}

.main-content {
  flex-grow: 1;
  padding-top: var(--navbar-height, 60px); /* 为固定导航栏留出空间，假设导航栏高度为 CSS 变量 */
  /* 可以根据需要添加底部内边距，如果页脚也是固定定位的话 */
  /* padding-bottom: var(--footer-height, 50px); */
  overflow-x: hidden; /* 防止内容溢出导致水平滚动条 */
}

.main-content-inner-container {
  max-width: var(--layout-max-width, 1280px); /* 使用CSS变量控制最大宽度 */
  margin: 0 auto; /* 水平居中 */
  padding: 2rem 1.5rem; /* 内容区域的内边距，可以根据需要调整 */
  width: 100%;
}

/* 页面切换过渡动画 */
.page-transition-enter-active,
.page-transition-leave-active {
  transition: opacity 0.25s ease-out, transform 0.25s ease-out;
}

.page-transition-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-transition-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>