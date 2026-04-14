<template>
  <section class="auth-app">
    <div class="bg-top">
      <div class="brand">
        <div class="logo">S</div>
        <h1>StudyHub</h1>
        <p>{{ tab === "login" ? "欢迎回来" : "创建你的账号" }}</p>
      </div>
    </div>

    <div class="form-wrap">
      <div class="panel-title">{{ tab === "login" ? "账号登录" : "账号注册" }}</div>
      <div v-if="tab === 'login'" class="panel">
        <el-input v-model="loginForm.phone" placeholder="手机号" size="large" />
        <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password />
        <el-button type="primary" class="main-btn app-btn" @click="login">登录</el-button>
        <div class="switch-inline">
          <span>还没有账号？</span>
          <button class="link-btn" @click="tab = 'register'">立即注册</button>
        </div>
      </div>

      <div v-else class="panel">
        <el-input v-model="registerForm.phone" placeholder="手机号" size="large" />
        <el-input v-model="registerForm.nickname" placeholder="昵称" size="large" />
        <el-input v-model="registerForm.password" type="password" placeholder="密码" size="large" show-password />
        <div class="code-row">
          <el-input v-model="registerForm.code" placeholder="验证码" size="large" />
          <el-button class="mini-btn" @click="sendCode">获取</el-button>
        </div>
        <el-button type="primary" class="main-btn app-btn" @click="register">注册</el-button>
        <div class="switch-inline">
          <span>已有账号？</span>
          <button class="link-btn" @click="tab = 'login'">去登录</button>
        </div>
      </div>

      <div class="tips">演示账号：13800000002 / 123456</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { apiPost } from "../../api/http";
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const tab = ref("login");

const loginForm = reactive({ phone: "13800000002", password: "123456" });
const registerForm = reactive({ phone: "", nickname: "", password: "", code: "" });

const login = async () => {
  try {
    const data = await apiPost<{ token: string; userId: number; nickname: string; role: number }>("/auth/login", loginForm);
    auth.setLogin(data.token, { userId: data.userId, nickname: data.nickname, role: data.role });
    ElMessage.success("登录成功");
    router.push(String(route.query.redirect || "/home"));
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "登录失败");
  }
};

const sendCode = async () => {
  try {
    const data = await apiPost<{ code: string }>(`/auth/send-code?phone=${registerForm.phone}`);
    ElMessage.success(`模拟验证码: ${data.code}`);
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "发送失败");
  }
};

const register = async () => {
  try {
    const data = await apiPost<{ token: string; userId: number; nickname: string }>("/auth/register", registerForm);
    auth.setLogin(data.token, { userId: data.userId, nickname: data.nickname, role: 1 });
    ElMessage.success("注册成功");
    router.push(String(route.query.redirect || "/home"));
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "注册失败");
  }
};
</script>

<style scoped>
.auth-app {
  min-height: calc(100vh - 90px);
  display: flex;
  flex-direction: column;
  padding: 0 0 90px;
  position: relative;
  background: #f8fafc;
}
.bg-top {
  background: linear-gradient(160deg, #0f766e, #115e59);
  min-height: 260px;
  padding: 22px 16px 28px;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}
.brand {
  text-align: center;
  color: #fff;
  margin-top: 4px;
}
.logo {
  width: 52px;
  height: 52px;
  margin: 0 auto 8px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 24px;
  border: 1px solid rgba(255, 255, 255, 0.25);
}
.brand h1 {
  margin: 0;
  font-size: 24px;
}
.brand p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
}
.form-wrap {
  background: #fff;
  border-radius: 18px;
  padding: 14px 12px 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  margin: -42px 12px 0;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 10px;
}
.panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}
.main-btn {
  width: 100%;
  height: 42px;
  border-radius: 10px;
  font-weight: 600;
}
.app-btn.el-button--primary {
  --el-button-bg-color: #0f766e;
  --el-button-border-color: #0f766e;
  --el-button-hover-bg-color: #0d9488;
  --el-button-hover-border-color: #0d9488;
  --el-button-active-bg-color: #115e59;
  --el-button-active-border-color: #115e59;
}
.mini-btn {
  border-radius: 10px;
  height: 40px;
}
.tips {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}
.switch-inline {
  margin-top: 2px;
  text-align: center;
  font-size: 13px;
  color: #64748b;
}
.link-btn {
  margin-left: 4px;
  border: none;
  background: transparent;
  color: #0f766e;
  font-weight: 600;
  padding: 0;
}
</style>
