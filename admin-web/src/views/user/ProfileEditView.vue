<template>
  <section class="profile-page">
    <div class="header-row">
      <el-button text @click="back">返回</el-button>
      <h2>编辑个人信息</h2>
    </div>

    <article class="card-block">
      <h3>基础信息</h3>
      <el-form :model="profileForm" label-width="90px">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-button type="primary" @click="saveProfile">保存信息</el-button>
      </el-form>
    </article>

    <article class="card-block">
      <h3>修改密码</h3>
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-button type="primary" @click="savePassword">更新密码</el-button>
      </el-form>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet, apiPost, apiPut } from "../../api/http";
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const auth = useAuthStore();

const profileForm = reactive({
  nickname: ""
});

const pwdForm = reactive({
  oldPassword: "",
  newPassword: ""
});

const loadProfile = async () => {
  try {
    const data = await apiGet<{ nickname: string }>("/users/profile");
    profileForm.nickname = data.nickname || "";
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载个人信息失败");
  }
};

const saveProfile = async () => {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning("请输入昵称");
    return;
  }
  try {
    await apiPut("/users/profile", { nickname: profileForm.nickname.trim() });
    auth.updateNickname(profileForm.nickname.trim());
    ElMessage.success("个人信息已更新");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "保存失败");
  }
};

const savePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning("请填写完整密码信息");
    return;
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning("新密码至少 6 位");
    return;
  }
  try {
    await apiPost("/users/password", {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    });
    pwdForm.oldPassword = "";
    pwdForm.newPassword = "";
    ElMessage.success("密码修改成功");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "密码修改失败");
  }
};

const back = () => router.push("/admin");

onMounted(loadProfile);
</script>

<style scoped>
.profile-page {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.header-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-row h2 {
  margin: 0;
  font-size: 20px;
}
.card-block {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #dbe7e4;
}
.card-block h3 {
  margin: 0 0 12px;
  font-size: 16px;
}
</style>
