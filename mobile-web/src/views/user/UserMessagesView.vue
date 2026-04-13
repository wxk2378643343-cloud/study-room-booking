<template>
  <section class="page card">
    <div class="head">
      <el-button text @click="router.back()">返回</el-button>
      <h2>消息中心</h2>
    </div>
    <div class="tabs">
      <button :class="{ active: tab === 'unread' }" @click="tab = 'unread'">未读</button>
      <button :class="{ active: tab === 'read' }" @click="tab = 'read'">已读</button>
      <button :class="{ active: tab === 'all' }" @click="tab = 'all'">全部</button>
    </div>
    <div class="list">
      <div v-if="filtered.length === 0" class="empty">暂无消息</div>
      <div v-for="n in filtered" :key="n.id" class="item">
        <div class="line1">
          <span>{{ n.title }}</span>
          <el-tag size="small" :type="n.is_read ? 'info' : 'warning'">{{ n.is_read ? "已读" : "未读" }}</el-tag>
        </div>
        <div class="line2">{{ n.content }}</div>
        <div class="line3">
          <span>{{ n.created_at }}</span>
          <el-button v-if="!n.is_read" size="small" @click="markRead(n.id)">标记已读</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet, apiPost } from "../../api/http";

const route = useRoute();
const router = useRouter();
const tab = ref((route.query.tab as "unread" | "read" | "all") || "unread");
const list = ref<any[]>([]);

const load = async () => {
  try {
    list.value = await apiGet("/users/notifications");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载失败");
  }
};

const filtered = computed(() => {
  if (tab.value === "unread") return list.value.filter((n) => !n.is_read);
  if (tab.value === "read") return list.value.filter((n) => !!n.is_read);
  return list.value;
});

const markRead = async (id: number) => {
  await apiPost(`/users/notifications/${id}/read`);
  await load();
};

load();
</script>

<style scoped>
.page { min-height: calc(100vh - 110px); }
.head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.head h2 { margin: 0; font-size: 18px; }
.tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 10px; }
.tabs button { height: 34px; border-radius: 10px; border: 1px solid #d1fae5; background: #ecfdf5; color: #0f766e; }
.tabs button.active { background: #0f766e; color: #fff; border-color: #0f766e; }
.list { display: flex; flex-direction: column; gap: 8px; }
.item { border: 1px solid #e8edf3; border-radius: 12px; padding: 10px; background: #fff; }
.line1 { display: flex; justify-content: space-between; align-items: center; gap: 8px; font-size: 13px; }
.line2 { margin-top: 4px; font-size: 12px; color: #475569; }
.line3 { margin-top: 6px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #64748b; }
.empty { text-align: center; color: #64748b; padding: 14px 0; font-size: 13px; }
</style>
