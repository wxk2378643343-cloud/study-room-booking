<template>
  <section class="detail-page">
    <div class="hero">
      <button class="back-btn" @click="back">< 返回</button>
      <h2>{{ roomName || "自习室详情" }}</h2>
      <p>选择日期和时段后，点击绿色座位完成预约</p>
    </div>

    <div class="panel">
      <div class="row">
        <span class="label">预约日期</span>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" class="date-picker" />
      </div>
      <div class="row">
        <span class="label">时间段</span>
        <div class="slot-scroll">
          <button
            v-for="item in slotOptions"
            :key="item.code"
            class="slot-chip"
            :class="{ active: slotCode === item.code }"
            @click="slotCode = item.code"
          >
            <strong>{{ item.name }}</strong>
            <span>{{ item.time }}</span>
          </button>
        </div>
      </div>
    </div>

    <div class="panel seat-panel">
      <div class="legend">
        <span><i class="dot free"></i>空闲</span>
        <span><i class="dot booked"></i>已订</span>
        <span><i class="dot leave"></i>离开</span>
        <span><i class="dot closed"></i>关闭</span>
      </div>
      <div class="seat-grid">
        <button
          v-for="seat in seatSlots"
          :key="seat.seat_id"
          class="seat-btn"
          :class="seatClass(seat.slot_status)"
          @click="onSeatClick(seat)"
        >
          {{ seat.seat_no }}
        </button>
      </div>
    </div>

    <el-dialog v-model="reportVisible" title="举报占座" width="320px" class="report-dialog">
      <div class="report-tip">请尽量描述现场情况，便于管理员快速核实处理。</div>
      <el-form :model="reportForm" label-width="84px" class="report-form">
        <el-form-item label="座位号">
          <el-input :model-value="reportForm.seatNo" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="reportForm.description" type="textarea" :rows="3" placeholder="请输入举报描述" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="reportForm.imageUrl" placeholder="可选：现场照片链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button class="report-btn" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet, apiPost } from "../../api/http";

type SeatSlot = { seat_id: number; seat_no: string; slot_status: number };

const route = useRoute();
const router = useRouter();
const roomId = Number(route.params.roomId);
const roomName = ref("");
const date = ref(new Date().toISOString().slice(0, 10));
const slotCode = ref(1);
const seatSlots = ref<SeatSlot[]>([]);
const reportVisible = ref(false);
const reportForm = ref({
  seatId: null as number | null,
  seatNo: "",
  description: "",
  imageUrl: ""
});
const slotOptions = [
  { code: 1, name: "早1", time: "08:00-10:00" },
  { code: 2, name: "早2", time: "10:00-12:00" },
  { code: 3, name: "中1", time: "13:00-15:00" },
  { code: 4, name: "中2", time: "15:00-17:00" },
  { code: 5, name: "晚1", time: "18:00-20:00" },
  { code: 6, name: "晚2", time: "20:00-22:00" }
];

const loadDetail = async () => {
  try {
    const data = await apiGet<{ room: { name: string } }>(`/rooms/${roomId}`);
    roomName.value = data.room.name;
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载自习室详情失败");
  }
};

const loadSlots = async () => {
  try {
    seatSlots.value = await apiGet<SeatSlot[]>(`/rooms/${roomId}/seat-slots`, {
      date: date.value,
      slotCode: slotCode.value
    });
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载座位失败");
  }
};

const book = async (seat: SeatSlot) => {
  try {
    const data = await apiPost<{ orderNo: string }>("/bookings", {
      roomId,
      seatId: seat.seat_id,
      date: date.value,
      slotCode: slotCode.value
    });
    ElMessage.success(`预约成功，订单:${data.orderNo}`);
    await loadSlots();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "预约失败");
  }
};

const onSeatClick = async (seat: SeatSlot) => {
  if (seat.slot_status === 0) {
    await book(seat);
    return;
  }
  reportForm.value = {
    seatId: seat.seat_id,
    seatNo: seat.seat_no,
    description: "",
    imageUrl: ""
  };
  reportVisible.value = true;
};

const submitReport = async () => {
  if (!reportForm.value.seatId) {
    ElMessage.error("缺少座位信息，无法举报");
    return;
  }
  if (!reportForm.value.description.trim()) {
    ElMessage.warning("请填写举报描述");
    return;
  }
  try {
    await apiPost("/users/reports", {
      orderId: null,
      seatId: reportForm.value.seatId,
      imageUrl: reportForm.value.imageUrl.trim(),
      description: reportForm.value.description.trim()
    });
    reportVisible.value = false;
    ElMessage.success("举报已提交");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "提交举报失败");
  }
};

const back = () => router.push("/rooms");

const seatClass = (status: number) => {
  if (status === 0) return "free";
  if (status === 1) return "booked";
  if (status === 2) return "leave";
  return "closed";
};

watch([date, slotCode], async () => {
  await loadSlots();
});

loadDetail();
loadSlots();
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hero {
  background: linear-gradient(140deg, #0f766e, #0e8f85);
  border-radius: 16px;
  color: #f0fdfa;
  padding: 14px;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.25);
}
.back-btn {
  border: none;
  background: transparent;
  color: rgba(240, 253, 250, 0.88);
  padding: 0;
  margin-bottom: 8px;
  font-size: 13px;
}
.hero h2 {
  margin: 0;
  font-size: 20px;
}
.hero p {
  margin: 6px 0 0;
  font-size: 12px;
  color: rgba(240, 253, 250, 0.88);
}
.panel {
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  border: 1px solid #dbe7e4;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
}
.row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.row + .row {
  margin-top: 12px;
}
.label {
  font-size: 13px;
  color: #475569;
}
.date-picker {
  width: 100%;
}
.slot-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.slot-scroll::-webkit-scrollbar {
  display: none;
}
.slot-chip {
  min-width: 92px;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 12px;
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  color: #334155;
}
.slot-chip strong {
  font-size: 12px;
}
.slot-chip span {
  font-size: 10px;
}
.slot-chip.active {
  border-color: #0f766e;
  background: #e7faf7;
  color: #0f766e;
}
.seat-panel {
  padding-top: 12px;
}
.legend {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  row-gap: 8px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #475569;
}
.legend span {
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.seat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.seat-btn {
  border: none;
  padding: 11px 4px;
  color: #fff;
  border-radius: 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}
.seat-btn:disabled {
  cursor: not-allowed;
  opacity: 0.95;
}
.free {
  background: #16a34a;
}
.booked {
  background: #dc2626;
}
.leave {
  background: #d97706;
}
.closed {
  background: #6b7280;
}
.report-btn {
  border: none;
  color: #fff;
  background: #d97706;
  border-radius: 10px;
}
.report-tip {
  margin-bottom: 10px;
  padding: 9px 10px;
  border-radius: 10px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  font-size: 12px;
}
.report-form :deep(.el-textarea__inner),
.report-form :deep(.el-input__wrapper) {
  border-radius: 10px;
}
.report-dialog :deep(.el-dialog) {
  width: 320px !important;
  max-width: calc(100vw - 56px);
  border-radius: 16px;
  overflow: hidden;
}
.report-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 14px 16px 12px;
  background: linear-gradient(140deg, #b45309, #d97706);
}
.report-dialog :deep(.el-dialog__title) {
  color: #fff7ed;
  font-weight: 700;
}
.report-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff7ed;
}
.report-dialog :deep(.el-dialog__body) {
  padding: 14px 16px 8px;
}
.report-dialog :deep(.el-dialog__footer) {
  padding: 0 16px 14px;
}
</style>
