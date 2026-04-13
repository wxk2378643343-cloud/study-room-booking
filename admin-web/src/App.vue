<template>
  <div v-if="isPlainPage" class="plain-layout">
    <RouterView />
  </div>
  <div v-else class="layout">
    <header class="topbar">
      <h1>StudyHub 管理端</h1>
      <nav>
        <el-dropdown v-if="auth.isLogin" trigger="hover" @command="handleCommand">
          <button class="profile-btn">
            <span class="avatar-circle">{{ avatarText }}</span>
            <span class="nickname">{{ displayName }}</span>
            <span class="arrow">▾</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">编辑个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>
    </header>
    <main class="container">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "./stores/auth";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const isPlainPage = computed(() => Boolean(route.meta.plain));
const avatarText = computed(() => {
  const text = auth.user?.nickname?.trim() || "A";
  return text.slice(0, 1).toUpperCase();
});
const displayName = computed(() => auth.user?.nickname?.trim() || "管理员");

const logout = () => {
  auth.logout();
  router.push("/login");
};

const handleCommand = (command: string) => {
  if (command === "profile") {
    router.push("/profile");
    return;
  }
  if (command === "logout") {
    logout();
  }
};
</script>

<style scoped>
.profile-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 4px 10px 4px 6px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #ecfeff;
  font-weight: 600;
  cursor: pointer;
}
.avatar-circle {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-grid;
  place-items: center;
  background: #ffffff;
  color: #0f766e;
  font-weight: 700;
}
.nickname {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow {
  font-size: 12px;
  opacity: 0.9;
}
</style>
