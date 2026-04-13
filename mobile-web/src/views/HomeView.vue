<template>
  <section class="home-page">
    <div class="hero">
      <div>
        <h2>StudyHub</h2>
        <p>自习更专注，预约更轻松</p>
      </div>
      <el-button size="small" @click="loadHealth">连接检测</el-button>
    </div>
    <div class="status-pill" v-if="message">{{ message }}</div>

    <div class="quick-grid">
      <button class="quick-card" @click="go('/rooms')">
        <span class="icon">📚</span>
        <span>找自习室</span>
      </button>
      <button class="quick-card" @click="go('/booking')">
        <span class="icon">🧾</span>
        <span>我的预约</span>
      </button>
      <button class="quick-card" @click="go('/user')">
        <span class="icon">👤</span>
        <span>个人中心</span>
      </button>
      <button class="quick-card" @click="go('/login')">
        <span class="icon">🔐</span>
        <span>登录注册</span>
      </button>
    </div>

    <div class="card chart-card">
      <div class="card-title">本周预约趋势</div>
      <div ref="trendRef" class="chart"></div>
    </div>

    <div class="card chart-card">
      <div class="card-title">预约状态占比</div>
      <div ref="pieRef" class="chart"></div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import * as echarts from "echarts";
import { apiGet } from "../api/http";

const router = useRouter();
const message = ref("");
const trendRef = ref<HTMLDivElement | null>(null);
const pieRef = ref<HTMLDivElement | null>(null);
let trendChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;

const go = (path: string) => router.push(path);

const loadHealth = async () => {
  try {
    const data = await apiGet<{ service: string; status: string; time: string }>("/health");
    message.value = `服务 ${data.status} · ${data.time}`;
  } catch {
    message.value = "后端暂不可用";
  }
};

const renderCharts = async () => {
  await nextTick();
  if (trendRef.value) {
    trendChart = echarts.init(trendRef.value);
    trendChart.setOption({
      grid: { left: 30, right: 10, top: 20, bottom: 24 },
      xAxis: { type: "category", data: ["一", "二", "三", "四", "五", "六", "日"] },
      yAxis: { type: "value" },
      series: [{ type: "line", smooth: true, data: [6, 9, 8, 12, 15, 11, 10], areaStyle: {} }],
      color: ["#0f766e"]
    });
  }
  if (pieRef.value) {
    pieChart = echarts.init(pieRef.value);
    pieChart.setOption({
      tooltip: { trigger: "item" },
      series: [
        {
          type: "pie",
          radius: ["40%", "70%"],
          data: [
            { name: "待签到", value: 6 },
            { name: "使用中", value: 3 },
            { name: "已签退", value: 9 },
            { name: "已取消", value: 2 }
          ]
        }
      ],
      color: ["#f59e0b", "#0ea5e9", "#22c55e", "#94a3b8"]
    });
  }
};

onMounted(async () => {
  await loadHealth();
  await renderCharts();
  window.addEventListener("resize", resizeCharts);
});

const resizeCharts = () => {
  trendChart?.resize();
  pieChart?.resize();
};

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  trendChart?.dispose();
  pieChart?.dispose();
});
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hero {
  background: linear-gradient(135deg, #0f766e, #115e59);
  border-radius: 12px;
  color: #fff;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.hero h2 {
  margin: 0;
  font-size: 20px;
}
.hero p {
  margin: 4px 0 0;
  font-size: 13px;
  opacity: 0.95;
}
.status-pill {
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  color: #0e7490;
  padding: 8px 10px;
  border-radius: 10px;
  font-size: 12px;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.quick-card {
  border: none;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
  padding: 10px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #334155;
}
.icon {
  font-size: 20px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}
.chart {
  height: 180px;
}
</style>
