<template>
  <section class="mine-page">
    <div class="profile-card">
      <button class="settings-trigger" @click="settingsVisible = true">设置</button>
      <div class="avatar">{{ avatarText }}</div>
      <div class="profile-meta">
        <div class="nickname">{{ profile.nickname || "未设置昵称" }}</div>
        <div class="phone">{{ profile.phone || "-" }}</div>
      </div>
    </div>

    <div class="stats-grid">
      <button class="stat-card" @click="openRecords('points')">
        <div class="label">积分</div>
        <div class="value">{{ points.points_balance || 0 }}</div>
      </button>
      <button class="stat-card" @click="openRecords('violation')">
        <div class="label">违规</div>
        <div class="value">{{ profile.violation_count || 0 }}</div>
      </button>
      <button class="stat-card" @click="openMessages('unread')">
        <div class="label">未读消息</div>
        <div class="value">{{ unreadCount }}</div>
      </button>
    </div>

    <div class="menu-card">
      <div class="menu-title">我的服务</div>
      <button class="menu-item" @click="openRecords('coupon')">
        <span>我的优惠券</span>
        <span class="suffix">{{ coupons.length }} 张</span>
      </button>
      <button class="menu-item" @click="openRecords('points')">
        <span>积分流水</span>
        <span class="suffix">{{ pointLogs.length }} 条</span>
      </button>
      <button class="menu-item" @click="openRecords('violation')">
        <span>违规记录</span>
        <span class="suffix">{{ violations.length }} 条</span>
      </button>
      <button class="menu-item" @click="openMessages('all')">
        <span>消息中心</span>
        <span class="suffix">查看全部</span>
      </button>
    </div>

    <el-drawer v-model="settingsVisible" :with-header="false" direction="rtl" size="84%">
      <div class="drawer-head">
        <div class="drawer-title">设置</div>
        <button class="close-btn" @click="settingsVisible = false">×</button>
      </div>
      <div class="drawer-content">
        <button class="drawer-row" @click="openEditProfile">
          <span>修改个人信息</span>
          <span>›</span>
        </button>
        <button class="drawer-row" @click="openPasswordDialog">
          <span>修改密码</span>
          <span>›</span>
        </button>
        <button class="drawer-row" @click="openMessages('all')">
          <span>消息中心</span>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" />
        </button>
        <button class="drawer-row danger" @click="logout">
          <span>退出登录</span>
          <span>›</span>
        </button>
      </div>
    </el-drawer>

    <el-drawer v-model="messagesVisible" :with-header="false" direction="rtl" size="90%">
      <div class="drawer-head">
        <div class="drawer-title">消息中心</div>
        <button class="close-btn" @click="messagesVisible = false">×</button>
        <div class="tabs">
          <button :class="{ active: messageTab === 'unread' }" @click="messageTab = 'unread'">未读</button>
          <button :class="{ active: messageTab === 'read' }" @click="messageTab = 'read'">已读</button>
          <button :class="{ active: messageTab === 'all' }" @click="messageTab = 'all'">全部</button>
        </div>
      </div>
      <div class="drawer-content">
        <div v-if="filteredMessages.length === 0" class="empty">暂无消息</div>
        <div v-for="msg in filteredMessages" :key="msg.id" class="list-card">
          <div class="line1">
            <span>{{ msg.title }}</span>
            <el-tag size="small" :type="msg.is_read ? 'info' : 'warning'">{{ msg.is_read ? "已读" : "未读" }}</el-tag>
          </div>
          <div class="line2">{{ msg.content }}</div>
          <div class="line3">
            <span>{{ msg.created_at }}</span>
            <el-button v-if="!msg.is_read" size="small" @click="markRead(msg.id)">标记已读</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="recordsVisible" :with-header="false" direction="rtl" size="90%">
      <div class="drawer-head">
        <div class="drawer-title">{{ recordTitle }}</div>
        <button class="close-btn" @click="recordsVisible = false">×</button>
        <div class="tabs">
          <button :class="{ active: recordTab === 'coupon' }" @click="recordTab = 'coupon'">优惠券</button>
          <button :class="{ active: recordTab === 'points' }" @click="recordTab = 'points'">积分</button>
          <button :class="{ active: recordTab === 'violation' }" @click="recordTab = 'violation'">违规</button>
        </div>
      </div>
      <div class="drawer-content">
        <template v-if="recordTab === 'coupon'">
          <div v-if="coupons.length === 0" class="empty">暂无优惠券</div>
          <div v-for="c in coupons" :key="c.id" class="list-card">
            <div class="line1">
              <span>{{ c.template_name }}</span>
              <el-tag size="small" :type="couponTag(c.status)">{{ couponText(c.status) }}</el-tag>
            </div>
            <div class="line2">到期：{{ c.expire_at }}</div>
          </div>
        </template>
        <template v-if="recordTab === 'points'">
          <div v-if="pointLogs.length === 0" class="empty">暂无积分流水</div>
          <div v-for="p in pointLogs" :key="p.id" class="list-card">
            <div class="line1">
              <span>{{ pointsBizText(p.biz_type) }}</span>
              <el-tag size="small" :type="p.change_amount >= 0 ? 'success' : 'danger'">
                {{ p.change_amount > 0 ? "+" : "" }}{{ p.change_amount }}
              </el-tag>
            </div>
            <div class="line2">{{ p.created_at }}</div>
          </div>
        </template>
        <template v-if="recordTab === 'violation'">
          <div v-if="violations.length === 0" class="empty">暂无违规</div>
          <div v-for="v in violations" :key="v.id" class="list-card">
            <div class="line1">
              <span>{{ violationText(v.violation_type) }}</span>
              <span>{{ v.created_at }}</span>
            </div>
            <div class="line2">{{ v.remark || "-" }}</div>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="profileVisible" title="修改个人信息" width="320px">
      <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
      <div class="dialog-actions">
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button class="brand-btn" type="primary" @click="saveProfile">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="修改密码" width="320px">
      <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="原密码" />
      <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="新密码（至少6位）" style="margin-top:8px;" />
      <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="确认新密码" style="margin-top:8px;" />
      <div class="dialog-actions">
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button class="brand-btn" type="primary" @click="savePassword">更新</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet, apiPost, apiPut } from "../../api/http";
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const auth = useAuthStore();

const profile = reactive<Record<string, any>>({});
const points = reactive<Record<string, any>>({});
const coupons = ref<any[]>([]);
const pointLogs = ref<any[]>([]);
const violations = ref<any[]>([]);
const notifications = ref<any[]>([]);
const unreadCount = ref(0);

const settingsVisible = ref(false);
const messagesVisible = ref(false);
const recordsVisible = ref(false);
const profileVisible = ref(false);
const passwordVisible = ref(false);

const messageTab = ref<"unread" | "read" | "all">("unread");
const recordTab = ref<"coupon" | "points" | "violation">("coupon");

const profileForm = reactive({ nickname: "" });
const passwordForm = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });

const avatarText = computed(() => {
  const name = String(profile.nickname || "").trim();
  if (name) return name.slice(0, 1).toUpperCase();
  const phone = String(profile.phone || "");
  return phone ? phone.slice(-1) : "U";
});

const filteredMessages = computed(() => {
  if (messageTab.value === "unread") return notifications.value.filter((n) => !n.is_read);
  if (messageTab.value === "read") return notifications.value.filter((n) => !!n.is_read);
  return notifications.value;
});

const recordTitle = computed(() => {
  return { coupon: "我的优惠券", points: "积分流水", violation: "违规记录" }[recordTab.value];
});

const load = async () => {
  try {
    Object.assign(profile, await apiGet("/users/profile"));
    Object.assign(points, await apiGet("/users/points"));
    coupons.value = await apiGet("/users/coupons");
    pointLogs.value = await apiGet("/users/points/logs");
    violations.value = await apiGet("/users/violations");
    notifications.value = await apiGet("/users/notifications");
    unreadCount.value = notifications.value.filter((n) => !n.is_read).length;
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载失败");
  }
};

const openMessages = (tab: "unread" | "read" | "all") => {
  settingsVisible.value = false;
  messageTab.value = tab;
  messagesVisible.value = true;
};

const openRecords = (tab: "coupon" | "points" | "violation") => {
  recordTab.value = tab;
  recordsVisible.value = true;
};

const openEditProfile = () => {
  settingsVisible.value = false;
  profileForm.nickname = profile.nickname || "";
  profileVisible.value = true;
};

const openPasswordDialog = () => {
  settingsVisible.value = false;
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordVisible.value = true;
};

const saveProfile = async () => {
  try {
    await apiPut("/users/profile", { nickname: profileForm.nickname });
    ElMessage.success("保存成功");
    profileVisible.value = false;
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "保存失败");
  }
};

const savePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }
  try {
    await apiPost("/users/password", {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    ElMessage.success("密码已更新，请重新登录");
    passwordVisible.value = false;
    auth.logout();
    router.push("/login");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "修改密码失败");
  }
};

const markRead = async (id: number) => {
  await apiPost(`/users/notifications/${id}/read`);
  await load();
};

const logout = () => {
  auth.logout();
  router.push("/login");
};

const couponText = (s: number) => ({ 0: "未使用", 1: "已使用", 2: "已过期" }[s] || "未知");
const couponTag = (s: number): "success" | "info" | "warning" => ({ 0: "success", 1: "info", 2: "warning" }[s] || "info");
const violationText = (t: number) => ({ 1: "迟到", 2: "占座", 3: "离开超时" }[t] || `类型${t}`);
const pointsBizText = (t: number) => ({ 1: "按时签退奖励", 2: "兑换扣减", 3: "管理员调整" }[t] || `类型${t}`);

load();
</script>

<style scoped>
.mine-page { display: flex; flex-direction: column; gap: 12px; }
.profile-card {
  background: linear-gradient(150deg, #0f766e, #115e59);
  border-radius: 18px;
  padding: 16px 14px;
  color: #fff;
  display: flex;
  gap: 10px;
  align-items: center;
  position: relative;
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.28);
}
.settings-trigger {
  position: absolute; right: 10px; top: 10px;
  border: 1px solid rgba(255,255,255,.35);
  background: rgba(255,255,255,.12);
  color: #fff; height: 28px; padding: 0 10px; border-radius: 999px; font-size: 12px;
}
.avatar { width: 52px; height: 52px; border-radius: 50%; background: rgba(255,255,255,.24); display: grid; place-items: center; font-size: 20px; font-weight: 700; }
.nickname { font-size: 18px; font-weight: 600; }
.phone { font-size: 12px; opacity: .92; margin-top: 2px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.stat-card {
  border: none; text-align: left; background: #fff; border-radius: 14px; padding: 12px 10px;
  box-shadow: 0 10px 20px rgba(15,23,42,.06);
}
.stat-card:active { transform: scale(.98); }
.label { font-size: 12px; color: #64748b; }
.value { margin-top: 4px; font-size: 15px; font-weight: 600; color: #0f172a; }
.menu-card { background: #fff; border-radius: 14px; padding: 10px 10px 4px; box-shadow: 0 10px 20px rgba(15,23,42,.06); }
.menu-title { font-size: 13px; font-weight: 600; margin: 4px 2px 8px; color: #64748b; }
.menu-item, .drawer-row {
  width: 100%; border: none; background: #fff; height: 48px;
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #f3f4f6; color: #0f172a;
}
.menu-item:last-child, .drawer-row:last-child { border-bottom: none; }
.suffix { color: #94a3b8; font-size: 12px; }
.drawer-row.danger { color: #dc2626; }
:deep(.el-drawer__body) { background: #f8fafc; padding: 0; }
.drawer-head {
  background: linear-gradient(140deg, #0f766e, #115e59);
  border-radius: 0 0 12px 12px;
  padding: 10px;
  color: #fff;
  position: relative;
}
.drawer-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.close-btn {
  position: absolute; top: 8px; right: 8px; width: 24px; height: 24px;
  border-radius: 50%; border: 1px solid rgba(255,255,255,.35); background: rgba(255,255,255,.12); color: #fff;
  font-size: 16px; line-height: 20px;
}
.tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.tabs button {
  height: 30px; border-radius: 8px; border: 1px solid rgba(255,255,255,.35);
  background: rgba(255,255,255,.12); color: #d1fae5; font-size: 12px;
}
.tabs button.active { background: #fff; color: #0f766e; border-color: #fff; font-weight: 600; }
.drawer-content { padding: 10px; display: flex; flex-direction: column; gap: 10px; }
.list-card { border: 1px solid #e8edf3; border-radius: 12px; padding: 10px; background: #fff; }
.line1 { display: flex; justify-content: space-between; align-items: center; gap: 8px; font-size: 13px; }
.line2 { margin-top: 4px; font-size: 12px; color: #475569; }
.line3 { margin-top: 6px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #64748b; }
.empty { text-align: center; color: #64748b; padding: 14px 0; font-size: 13px; }
.dialog-actions { margin-top: 10px; display: flex; justify-content: flex-end; gap: 8px; }
.brand-btn.el-button--primary {
  --el-button-bg-color: #0f766e;
  --el-button-border-color: #0f766e;
  --el-button-hover-bg-color: #0d9488;
  --el-button-hover-border-color: #0d9488;
  --el-button-active-bg-color: #115e59;
  --el-button-active-border-color: #115e59;
}
</style>
