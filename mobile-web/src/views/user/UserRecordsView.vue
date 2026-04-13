<template>
  <section class="page card">
    <div class="head">
      <el-button text @click="router.back()">返回</el-button>
      <h2>{{ title }}</h2>
    </div>
    <div class="tabs">
      <button :class="{ active: type === 'coupon' }" @click="type = 'coupon'">优惠券</button>
      <button :class="{ active: type === 'violation' }" @click="type = 'violation'">违规</button>
      <button :class="{ active: type === 'points' }" @click="type = 'points'">积分</button>
    </div>

    <div class="list" v-if="type === 'coupon'">
      <div v-if="coupons.length === 0" class="empty">暂无优惠券</div>
      <div v-for="c in coupons" :key="c.id" class="item">
        <div class="line1"><span>{{ c.template_name }}</span><el-tag size="small" :type="couponTag(c.status)">{{ couponText(c.status) }}</el-tag></div>
        <div class="line2">到期：{{ c.expire_at }}</div>
      </div>
    </div>

    <div class="list" v-if="type === 'violation'">
      <div v-if="violations.length === 0" class="empty">暂无违规</div>
      <div v-for="v in violations" :key="v.id" class="item">
        <div class="line1"><span>{{ violationText(v.violation_type) }}</span><span>{{ v.created_at }}</span></div>
        <div class="line2">{{ v.remark || "-" }}</div>
      </div>
    </div>

    <div class="list" v-if="type === 'points'">
      <div v-if="pointLogs.length === 0" class="empty">暂无积分流水</div>
      <div v-for="p in pointLogs" :key="p.id" class="item">
        <div class="line1">
          <span>{{ pointsBizText(p.biz_type) }}</span>
          <el-tag size="small" :type="p.change_amount >= 0 ? 'success' : 'danger'">{{ p.change_amount > 0 ? "+" : "" }}{{ p.change_amount }}</el-tag>
        </div>
        <div class="line2">{{ p.created_at }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet } from "../../api/http";

const route = useRoute();
const router = useRouter();
const type = ref((route.query.type as "coupon" | "violation" | "points") || "coupon");
const coupons = ref<any[]>([]);
const violations = ref<any[]>([]);
const pointLogs = ref<any[]>([]);

const title = computed(() => ({ coupon: "我的优惠券", violation: "违规记录", points: "积分流水" }[type.value]));

const load = async () => {
  try {
    coupons.value = await apiGet("/users/coupons");
    violations.value = await apiGet("/users/violations");
    pointLogs.value = await apiGet("/users/points/logs");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载失败");
  }
};

const couponText = (s: number) => ({ 0: "未使用", 1: "已使用", 2: "已过期" }[s] || "未知");
const couponTag = (s: number): "success" | "info" | "warning" => ({ 0: "success", 1: "info", 2: "warning" }[s] || "info");
const violationText = (t: number) => ({ 1: "迟到", 2: "占座", 3: "离开超时" }[t] || `类型${t}`);
const pointsBizText = (t: number) => ({ 1: "按时签退奖励", 2: "兑换扣减", 3: "管理员调整" }[t] || `类型${t}`);

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
.empty { text-align: center; color: #64748b; padding: 14px 0; font-size: 13px; }
</style>
