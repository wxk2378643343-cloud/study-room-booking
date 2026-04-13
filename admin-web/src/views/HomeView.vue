<template>
  <section class="card">
    <h2>系统首页</h2>
    <p>毕设版已实现：信息展示、预约选座、签到签退、用户中心、管理端运营与统计。</p>
    <el-button type="primary" @click="loadHealth">检测后端连接</el-button>
    <p v-if="message" class="status">{{ message }}</p>
    <div class="links">
      <RouterLink to="/login">登录注册</RouterLink>
      <RouterLink to="/rooms">自习室选座</RouterLink>
      <RouterLink to="/booking">我的预约</RouterLink>
      <RouterLink to="/user">个人中心</RouterLink>
      <RouterLink to="/admin">管理台</RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { apiGet } from "../api/http";

const message = ref("");

const loadHealth = async () => {
  const data = await apiGet<{ service: string; status: string; time: string }>("/health");
  message.value = `后端状态: ${data.status} (${data.time})`;
};
</script>

<style scoped>
.links {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}
</style>
