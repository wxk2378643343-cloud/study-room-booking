<template>
  <section class="settings-page">
    <div class="hero">
      <el-button text class="back-btn" @click="router.back()">返回</el-button>
      <h2>设置</h2>
      <p>个人信息与账号安全</p>
    </div>

    <div class="block">
      <div class="title">个人信息</div>
      <el-input v-model="nickname" placeholder="昵称" />
      <el-button class="save-btn app-btn" type="primary" @click="saveProfile">保存昵称</el-button>
    </div>

    <div class="block">
      <div class="title">修改密码</div>
      <el-input v-model="oldPassword" type="password" show-password placeholder="原密码" />
      <el-input v-model="newPassword" type="password" show-password placeholder="新密码（至少6位）" style="margin-top:8px;" />
      <el-input v-model="confirmPassword" type="password" show-password placeholder="确认新密码" style="margin-top:8px;" />
      <el-button class="save-btn app-btn" type="primary" @click="savePassword">更新密码</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet, apiPost, apiPut } from "../../api/http";

const router = useRouter();
const nickname = ref("");
const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");

const load = async () => {
  const profile = await apiGet<Record<string, any>>("/users/profile");
  nickname.value = profile.nickname || "";
};

const saveProfile = async () => {
  try {
    await apiPut("/users/profile", { nickname: nickname.value });
    ElMessage.success("昵称已更新");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "保存失败");
  }
};

const savePassword = async () => {
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }
  try {
    await apiPost("/users/password", { oldPassword: oldPassword.value, newPassword: newPassword.value });
    ElMessage.success("密码已更新，请重新登录");
    router.push("/login");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "修改密码失败");
  }
};

load();
</script>

<style scoped>
.settings-page {
  min-height: calc(100vh - 100px);
}
.hero {
  background: linear-gradient(145deg, #0f766e, #115e59);
  border-radius: 14px;
  padding: 12px;
  color: #fff;
  margin-bottom: 10px;
}
.back-btn {
  color: #d1fae5;
  padding: 0;
  min-height: auto;
}
.hero h2 {
  margin: 4px 0 0;
  font-size: 18px;
}
.hero p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #ccfbf1;
}
.block {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
}
.title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}
.save-btn {
  margin-top: 10px;
  width: 100%;
  height: 40px;
}
.app-btn.el-button--primary {
  --el-button-bg-color: #0f766e;
  --el-button-border-color: #0f766e;
  --el-button-hover-bg-color: #0d9488;
  --el-button-hover-border-color: #0d9488;
  --el-button-active-bg-color: #115e59;
  --el-button-active-border-color: #115e59;
}
</style>
