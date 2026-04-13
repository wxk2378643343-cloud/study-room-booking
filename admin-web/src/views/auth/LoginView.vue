<template>
  <section class="login-page">
    <div class="login-shell">
      <aside class="brand-panel">
        <span class="badge">StudyHub Admin</span>
        <h1>自习室运营管理后台</h1>
        <p>统一处理房间、预约、规则与举报数据。</p>
      </aside>

      <div class="login-card">
        <h2>登录</h2>
        <p class="sub">请输入管理员账号信息</p>
        <el-form :model="loginForm" class="form">
          <el-form-item>
            <el-input v-model="loginForm.phone" placeholder="手机号" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" size="large" />
          </el-form-item>
          <el-button class="login-btn" @click="login">进入管理台</el-button>
        </el-form>
        <div class="hint-block">
          <span>演示账号</span>
          <strong>13800000001 / 123456</strong>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { apiPost } from "../../api/http";
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const auth = useAuthStore();
const loginForm = reactive({ phone: "13800000001", password: "123456" });

const login = async () => {
  try {
    const data = await apiPost<{ token: string; userId: number; nickname: string; role: number }>("/auth/login", loginForm);
    if (data.role !== 9) {
      ElMessage.error("当前账号不是管理员");
      return;
    }
    auth.setLogin(data.token, { userId: data.userId, nickname: data.nickname, role: data.role });
    ElMessage.success("登录成功");
    router.push("/admin");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "登录失败");
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at 20% 10%, rgba(94, 234, 212, 0.32), transparent 30%),
    radial-gradient(circle at 90% 80%, rgba(16, 185, 129, 0.22), transparent 36%),
    linear-gradient(145deg, #0f766e, #0b5b54);
}
.login-shell {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 18px;
}
.brand-panel {
  border-radius: 20px;
  padding: 36px 34px;
  color: #ecfeff;
  background: linear-gradient(155deg, rgba(15, 118, 110, 0.7), rgba(6, 78, 59, 0.55));
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(6px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
}
.badge {
  display: inline-block;
  border-radius: 999px;
  border: 1px solid rgba(204, 251, 241, 0.5);
  color: #ccfbf1;
  font-size: 12px;
  padding: 5px 10px;
  margin-bottom: 16px;
}
.brand-panel h1 {
  margin: 0 0 10px;
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: 0.5px;
}
.brand-panel p {
  margin: 0;
  font-size: 14px;
  color: rgba(236, 254, 255, 0.86);
  line-height: 1.7;
}
.login-card {
  width: 100%;
  background: #fff;
  border-radius: 20px;
  padding: 30px 24px 24px;
  box-shadow: 0 20px 48px rgba(2, 44, 34, 0.24);
}
.login-card h2 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}
.sub {
  margin: 8px 0 20px;
  color: #64748b;
  font-size: 13px;
}
.form {
  width: 100%;
}
.form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #dbe4e7 inset;
}
.form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0f766e inset;
}
.login-btn {
  width: 100%;
  border: none;
  border-radius: 12px;
  height: 44px;
  background: #0f766e;
  color: #fff;
  font-weight: 700;
  margin-top: 4px;
}
.hint-block {
  margin-top: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}
.hint-block strong {
  color: #0f172a;
  font-weight: 600;
}
@media (max-width: 860px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 440px;
  }
  .brand-panel {
    display: none;
  }
  .login-card {
    padding: 26px 20px 20px;
  }
}
</style>
