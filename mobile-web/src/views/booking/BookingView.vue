<template>
  <section class="booking-page">
    <div class="page-head">
      <h2>我的预约</h2>
      <p>支持扫码签到、临时离开和在线签退</p>
      <el-button class="checkin-btn" @click="openCheckinModal()">扫码签到</el-button>
      <input ref="qrInput" class="hidden-input" type="file" accept="image/*" @change="onQrFileChange" />
    </div>

    <div class="tab-row">
      <button
        v-for="item in tabOptions"
        :key="item.value"
        class="tab-chip"
        :class="{ active: activeTab === item.value }"
        @click="activeTab = item.value"
      >
        {{ item.label }}
        <span class="tab-count">{{ countByTab(item.value) }}</span>
      </button>
    </div>

    <div class="list-scroll">
      <el-empty v-if="filteredOrders.length === 0" description="暂无预约记录" />
      <article v-for="row in filteredOrders" :key="row.id" class="order-card">
        <div class="order-top">
          <strong>{{ row.room_name }}</strong>
          <el-tag size="small" :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </div>
        <div class="order-meta">
          <span>座位 {{ row.seat_no }}</span>
          <span>{{ row.booking_date }}</span>
          <span>{{ row.slot_text || slotText(row.slot_code) }}</span>
        </div>
        <div v-if="row.cancel_reason" class="order-row">
          <span class="label">原因</span>
          <span>{{ row.cancel_reason }}</span>
        </div>
        <div class="order-actions">
          <el-button v-if="row.status === 0" class="outline-btn" @click="openCheckinModal()">去签到</el-button>
          <el-button v-if="row.status === 0" class="text-btn" @click="cancel(row.id)">取消预约</el-button>
          <el-button v-if="row.status === 1" class="outline-btn" @click="leave(row.id)">临时离开</el-button>
          <el-button v-if="row.status === 1" class="outline-btn" @click="back(row.id)">返回座位</el-button>
          <el-button v-if="row.status === 1" class="brand-btn" @click="checkout(row.id)">签退</el-button>
          <el-button v-if="row.status === 0 || row.status === 1" class="warn-btn" @click="openReportModal(row)">举报占座</el-button>
        </div>
      </article>
    </div>

    <el-dialog v-model="checkinVisible" title="签到" width="360px">
      <div class="checkin-tip">请上传预约二维码图片，系统将自动识别并完成签到。</div>
      <div class="checkin-actions">
        <el-button class="brand-btn" @click="pickQrImage">上传二维码并签到</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="举报占座" width="320px" class="report-dialog">
      <div class="report-tip">请尽量描述现场情况，便于管理员快速核实处理。</div>
      <el-form :model="reportForm" label-width="84px" class="report-form">
        <el-form-item label="座位号">
          <el-input :model-value="reportForm.seatNo" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="reportForm.description" type="textarea" :rows="3" placeholder="请输入举报描述" />
        </el-form-item>
        <el-form-item label="现场图片">
          <div class="report-upload">
            <el-button class="outline-btn" @click="pickReportImage">上传图片</el-button>
            <span class="upload-tip">{{ reportForm.uploading ? "上传中..." : reportForm.imageUrl ? "已上传" : "选填，支持 jpg/png/webp" }}</span>
          </div>
          <img v-if="reportForm.imageUrl" :src="reportForm.imageUrl" alt="举报图片" class="report-preview" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button class="warn-btn" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
    <input ref="reportImageInput" class="hidden-input" type="file" accept="image/*" @change="onReportImageChange" />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { apiGet, apiPost, apiUpload } from "../../api/http";
import jsQR from "jsqr";

type Order = {
  id: number;
  seat_id?: number;
  room_name: string;
  seat_no: string;
  booking_date: string;
  slot_code: number;
  slot_text?: string;
  status: number;
  voucher_code: string;
  cancel_reason?: string;
};

const voucherCode = ref("");
const orders = ref<Order[]>([]);
const activeTab = ref("all");
const qrInput = ref<HTMLInputElement | null>(null);
const reportImageInput = ref<HTMLInputElement | null>(null);
const checkinVisible = ref(false);
const reportVisible = ref(false);
const reportForm = ref({
  orderId: null as number | null,
  seatId: null as number | null,
  seatNo: "",
  description: "",
  imageUrl: "",
  uploading: false
});
const tabOptions = [
  { label: "全部", value: "all" },
  { label: "待签到", value: "0" },
  { label: "使用中", value: "1" },
  { label: "已签退", value: "2" },
  { label: "已取消", value: "3" },
  { label: "爽约", value: "4" }
];

const load = async () => {
  try {
    orders.value = await apiGet<Order[]>("/bookings/mine");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载预约失败");
  }
};

const checkin = async () => {
  if (!voucherCode.value || !voucherCode.value.trim()) {
    ElMessage.warning("请先上传二维码");
    return;
  }
  try {
    await apiPost("/bookings/checkin", { voucherCode: voucherCode.value.trim() });
    ElMessage.success("签到成功");
    checkinVisible.value = false;
    await load();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "签到失败");
  }
};

const openCheckinModal = (voucher?: string) => {
  voucherCode.value = voucher || "";
  checkinVisible.value = true;
};

const pickQrImage = () => {
  qrInput.value?.click();
};

const onQrFileChange = async (event: Event) => {
  try {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const text = await decodeQrText(file);
    if (!text) {
      ElMessage.error("未识别到二维码，请换一张更清晰的图片");
      return;
    }
    voucherCode.value = text.trim();
    ElMessage.success("二维码识别成功，正在签到");
    await checkin();
  } catch {
    ElMessage.error("二维码解析失败");
  } finally {
    if (qrInput.value) {
      qrInput.value.value = "";
    }
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

const openReportModal = (row: Order) => {
  reportForm.value = {
    orderId: row.id,
    seatId: row.seat_id ?? null,
    seatNo: row.seat_no,
    description: "",
    imageUrl: "",
    uploading: false
  };
  reportVisible.value = true;
};

const pickReportImage = () => {
  reportImageInput.value?.click();
};

const onReportImageChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    reportForm.value.uploading = true;
    const formData = new FormData();
    formData.append("file", file);
    const data = await apiUpload<{ url: string }>("/users/report-images", formData);
    reportForm.value.imageUrl = data.url;
    ElMessage.success("图片上传成功");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "图片上传失败");
  } finally {
    reportForm.value.uploading = false;
    if (reportImageInput.value) {
      reportImageInput.value.value = "";
    }
  }
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
      orderId: reportForm.value.orderId,
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


const slotText = (code: number) => {
  const m: Record<number, string> = {
    1: "08:00-10:00",
    2: "10:00-12:00",
    3: "13:00-15:00",
    4: "15:00-17:00",
    5: "18:00-20:00",
    6: "20:00-22:00"
  };
  return m[code] || `时段${code}`;
};

const statusText = (status: number) => {
  const m: Record<number, string> = {
    0: "待签到",
    1: "使用中",
    2: "已签退",
    3: "已取消",
    4: "爽约"
  };
  return m[status] || String(status);
};

const statusTagType = (status: number): "primary" | "success" | "warning" | "danger" | "info" => {
  const m: Record<number, "primary" | "success" | "warning" | "danger" | "info"> = {
    0: "warning",
    1: "primary",
    2: "success",
    3: "info",
    4: "danger"
  };
  return m[status] || "info";
};

const filteredOrders = computed(() => {
  if (activeTab.value === "all") return orders.value;
  const target = Number(activeTab.value);
  return orders.value.filter((o) => o.status === target);
});

const countByTab = (tabValue: string) => {
  if (tabValue === "all") return orders.value.length;
  const target = Number(tabValue);
  return orders.value.filter((o) => o.status === target).length;
};

const decodeQrText = (file: File): Promise<string | null> => {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          resolve(null);
          return;
        }
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const result = jsQR(imageData.data, imageData.width, imageData.height);
        URL.revokeObjectURL(url);
        resolve(result?.data ?? null);
      } catch (e) {
        URL.revokeObjectURL(url);
        reject(e);
      }
    };
    img.onerror = (e) => {
      URL.revokeObjectURL(url);
      reject(e);
    };
    img.src = url;
  });
};

load();
</script>

<style scoped>
.booking-page {
  height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}
.page-head {
  background: linear-gradient(140deg, #0f766e, #0e8f85);
  border-radius: 16px;
  color: #f0fdfa;
  padding: 14px;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.25);
}
.page-head h2 {
  margin: 0;
  font-size: 20px;
}
.page-head p {
  margin: 6px 0 10px;
  font-size: 12px;
  color: rgba(240, 253, 250, 0.88);
}
.checkin-btn {
  border-radius: 999px;
  background: #ffffff;
  color: #0f766e;
  border: none;
  padding: 8px 14px;
  font-weight: 700;
}
.checkin-actions {
  display: flex;
  margin-top: 10px;
}
.checkin-tip {
  color: #475569;
  font-size: 13px;
}
.tab-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 0 2px 2px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.tab-row::-webkit-scrollbar {
  display: none;
}
.tab-chip {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 6px 11px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.tab-chip.active {
  border-color: #0f766e;
  background: #e7faf7;
  color: #0f766e;
}
.tab-count {
  font-size: 11px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.12);
}
.list-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.list-scroll::-webkit-scrollbar {
  display: none;
}
.order-card {
  margin-bottom: 10px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #dbe7e4;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
  padding: 12px;
}
.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.order-top strong {
  font-size: 16px;
  color: #0f172a;
}
.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.order-meta span {
  font-size: 12px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  padding: 3px 8px;
}
.order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}
.label {
  color: #64748b;
  font-size: 13px;
}
.order-actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.brand-btn {
  border: none;
  color: #fff;
  background: #0f766e;
  border-radius: 10px;
}
.outline-btn {
  border: 1px solid #0f766e;
  color: #0f766e;
  background: #fff;
  border-radius: 10px;
}
.text-btn {
  border: 1px solid #e2e8f0;
  color: #64748b;
  background: #fff;
  border-radius: 10px;
}
.warn-btn {
  border: none;
  color: #fff;
  background: #0f766e;
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
.report-upload {
  display: flex;
  align-items: center;
  gap: 8px;
}
.upload-tip {
  font-size: 12px;
  color: #64748b;
}
.report-preview {
  display: block;
  margin-top: 8px;
  width: 100%;
  max-height: 160px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #dbe7e4;
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
  background: linear-gradient(140deg, #0f766e, #0e8f85);
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
.hidden-input {
  display: none;
}
</style>
