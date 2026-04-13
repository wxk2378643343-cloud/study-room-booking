<template>
  <section class="card">
    <h2>管理端首页</h2>
    <el-tabs>
      <el-tab-pane label="自习室">
        <div class="toolbar">
          <el-button type="primary" @click="saveRoom">新增房间</el-button>
        </div>
        <el-table :data="rooms" border @row-click="onRoomClick">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="location_desc" label="位置" />
          <el-table-column prop="seat_total" label="座位数" width="90" />
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="座位管理">
        <p>当前房间: {{ currentRoomId || "-" }}</p>
        <el-button type="primary" @click="saveSeat">新增座位</el-button>
        <el-table :data="seats" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="seat_no" label="座位号" />
          <el-table-column prop="seat_type" label="类型" />
          <el-table-column prop="status" label="状态" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="预约规则">
        <p>当前房间: {{ currentRoomId || "-" }}</p>
        <el-form label-width="140px" class="rule-form">
          <el-form-item label="每天最大预约次数"><el-input v-model.number="rule.max_bookings_per_day" /></el-form-item>
          <el-form-item label="单次最大时长"><el-input v-model.number="rule.max_hours_per_booking" /></el-form-item>
          <el-form-item label="签到宽限分钟"><el-input v-model.number="rule.checkin_grace_minutes" /></el-form-item>
          <el-form-item label="临时离开分钟"><el-input v-model.number="rule.temp_leave_minutes" /></el-form-item>
        </el-form>
        <el-button type="primary" @click="saveRule">保存规则</el-button>
      </el-tab-pane>
      <el-tab-pane label="举报处理">
        <el-table :data="reports" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="reporter_phone" label="举报人" width="130" />
          <el-table-column prop="description_text" label="描述" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="handleReport(row.id, 1)">属实</el-button>
              <el-button size="small" @click="handleReport(row.id, 2)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="统计报表">
        <div class="toolbar">
          <el-date-picker v-model="from" type="date" value-format="YYYY-MM-DD" />
          <el-date-picker v-model="to" type="date" value-format="YYYY-MM-DD" />
          <el-button @click="loadDashboard">查询</el-button>
        </div>
        <p>预约总量: {{ dashboard.totalBookings || 0 }}</p>
        <p>已完成: {{ dashboard.completedBookings || 0 }}</p>
        <p>违规总量: {{ dashboard.violations || 0 }}</p>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { apiGet, apiPost } from "../../api/http";

const rooms = ref<any[]>([]);
const seats = ref<any[]>([]);
const reports = ref<any[]>([]);
const currentRoomId = ref<number | null>(null);
const rule = reactive<Record<string, any>>({});
const dashboard = reactive<Record<string, any>>({});
const from = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10));
const to = ref(new Date().toISOString().slice(0, 10));

const loadRooms = async () => {
  rooms.value = await apiGet("/admin/rooms");
  if (!currentRoomId.value && rooms.value.length > 0) {
    currentRoomId.value = rooms.value[0].id;
    await loadSeatsAndRule();
  }
};

const onRoomClick = async (row: any) => {
  currentRoomId.value = row.id;
  await loadSeatsAndRule();
};

const loadSeatsAndRule = async () => {
  if (!currentRoomId.value) return;
  seats.value = await apiGet(`/admin/rooms/${currentRoomId.value}/seats`);
  Object.assign(rule, await apiGet(`/admin/rooms/${currentRoomId.value}/rule`));
};

const saveRoom = async () => {
  await apiPost("/admin/rooms", {
    name: "新建自习室",
    locationDesc: "请编辑位置",
    facilities: "饮水",
    seatTotal: 0,
    status: 1
  });
  ElMessage.success("新增房间成功");
  await loadRooms();
};

const saveSeat = async () => {
  if (!currentRoomId.value) return;
  await apiPost(`/admin/rooms/${currentRoomId.value}/seats`, {
    seatNo: `N${Date.now().toString().slice(-4)}`,
    seatType: 1,
    xPos: 1,
    yPos: 1,
    status: 1
  });
  ElMessage.success("新增座位成功");
  await loadSeatsAndRule();
};

const saveRule = async () => {
  if (!currentRoomId.value) return;
  await apiPost(`/admin/rooms/${currentRoomId.value}/rule`, {
    maxBookingsPerDay: rule.max_bookings_per_day,
    maxHoursPerBooking: rule.max_hours_per_booking,
    checkinGraceMinutes: rule.checkin_grace_minutes,
    tempLeaveMinutes: rule.temp_leave_minutes
  });
  ElMessage.success("规则已保存");
};

const loadReports = async () => {
  reports.value = await apiGet("/admin/reports");
};

const handleReport = async (id: number, status: number) => {
  await apiPost(`/admin/reports/${id}/handle`, { status, note: "后台处理" });
  ElMessage.success("处理完成");
  await loadReports();
};

const loadDashboard = async () => {
  Object.assign(dashboard, await apiGet("/admin/dashboard", { from: from.value, to: to.value }));
};

const init = async () => {
  await loadRooms();
  await loadReports();
  await loadDashboard();
};
init();
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.rule-form {
  max-width: 500px;
}
</style>
