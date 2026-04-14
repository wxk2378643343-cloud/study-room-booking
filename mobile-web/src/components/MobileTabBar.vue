<template>
  <footer class="tabbar">
    <button
      v-for="item in tabs"
      :key="item.path"
      class="tab-item"
      :class="{ active: isActive(item.path) }"
      @click="go(item.path)"
    >
      <span class="tab-label">{{ item.label }}</span>
    </button>
  </footer>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const tabs = computed(() => [
  { label: "首页", path: "/home" },
  { label: "自习室", path: "/rooms" },
  { label: "预约", path: "/booking" },
  { label: auth.isLogin ? "我的" : "登录", path: auth.isLogin ? "/user" : "/login" }
]);

const isActive = (path: string) => {
  if (path === "/home") return route.path === "/home";
  return route.path.startsWith(path);
};

const go = (path: string) => {
  if (route.path !== path) router.push(path);
};
</script>

<style scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 60px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  z-index: 100;
}
.tab-item {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
}
.tab-item.active {
  color: #0f766e;
  font-weight: 600;
}
.tab-label {
  display: inline-block;
  transform: translateY(1px);
}
</style>
