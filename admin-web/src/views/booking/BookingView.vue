<template>
  <section class="card">
    <h2>我的预约 / 签到签退</h2>
    <div class="toolbar">
      <el-input v-model="voucherCode" placeholder="输入预约凭证码签到" style="max-width: 320px" />
      <el-button type="primary" @click="checkin">扫码签到</el-button>
      <el-button @click="runTimeoutJob">执行超时任务</el-button>
    </div>

    <el-table :data="orders" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="room_name" label="自习室" />
      <el-table-column prop="seat_no" label="座位" width="90" />
      <el-table-column prop="booking_date" label="日期" width="120" />
      <el-table-column prop="slot_code" label="时段" width="80" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="voucher_code" label="凭证码" />
      <el-table-column label="操作" width="360">
        <template #default="{ row }">
          <el-button size="small" @click="cancel(row.id)">取消</el-button>
          <el-button size="small" @click="leave(row.id)">临时离开</el-button>
          <el-button size="small" @click="back(row.id)">返回</el-button>
          <el-button size="small" type="success" @click="checkout(row.id)">签退</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { apiGet, apiPost } from "../../api/http";

type Order = { id: number; voucher_code: string };

const voucherCode = ref("");
const orders = ref<Order[]>([]);

const load = async () => {
  try {
    orders.value = await apiGet<Order[]>("/bookings/mine");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载预约失败");
  }
};

const checkin = async () => {
  if (!voucherCode.value || !voucherCode.value.trim()) {
    ElMessage.warning("请先输入预约凭证码");
    return;
  }
  try {
    await apiPost("/bookings/checkin", { voucherCode: voucherCode.value.trim() });
    ElMessage.success("签到成功");
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "签到失败");
  }
};

const cancel = async (id: number) => {
  try {
    await apiPost(`/bookings/${id}/cancel`);
    ElMessage.success("已取消");
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "取消失败");
  }
};

const leave = async (id: number) => {
  try {
    await apiPost(`/bookings/${id}/leave`);
    ElMessage.success("已登记临时离开");
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "操作失败");
  }
};

const back = async (id: number) => {
  try {
    await apiPost(`/bookings/${id}/return`);
    ElMessage.success("已返回");
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "操作失败");
  }
};

const checkout = async (id: number) => {
  try {
    await apiPost(`/bookings/${id}/checkout`);
    ElMessage.success("签退成功，已加积分");
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "签退失败");
  }
};

const runTimeoutJob = async () => {
  try {
    await apiPost("/bookings/jobs/run-timeout");
    ElMessage.success("超时任务执行完成");
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "任务执行失败");
  }
};

load();
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
