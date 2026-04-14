<template>
  <section class="admin-shell">
    <aside class="sidebar">
      <button
        v-for="item in menus"
        :key="item.key"
        class="menu-btn"
        :class="{ active: activeSection === item.key }"
        @click="switchSection(item.key)"
      >
        {{ item.label }}
      </button>
    </aside>

    <main class="content">
      <header class="content-head">
        <h3>{{ currentTitle }}</h3>
      </header>

      <section v-show="activeSection === 'dashboard'" class="panel">
        <div class="toolbar">
          <el-date-picker v-model="from" type="date" value-format="YYYY-MM-DD" />
          <el-date-picker v-model="to" type="date" value-format="YYYY-MM-DD" />
          <el-button type="primary" @click="loadDashboard">刷新数据</el-button>
        </div>
        <div class="stat-grid">
          <article class="stat-card">
            <span>预约总量</span>
            <strong>{{ dashboard.totalBookings || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>已完成</span>
            <strong>{{ dashboard.completedBookings || 0 }}</strong>
          </article>
          <article class="stat-card">
            <span>违规总量</span>
            <strong>{{ dashboard.violations || 0 }}</strong>
          </article>
        </div>
        <div class="chart-grid">
          <article class="chart-card">
            <h4>近12月预约趋势</h4>
            <div ref="trendChartRef" class="chart"></div>
          </article>
          <article class="chart-card">
            <h4>热门座位类型</h4>
            <div ref="seatTypeChartRef" class="chart"></div>
          </article>
        </div>
      </section>

      <section v-show="activeSection === 'rooms'" class="panel">
        <div class="toolbar">
          <el-button type="primary" @click="openRoomCreate">新增房间</el-button>
        </div>
        <el-table :data="rooms" border @row-click="onRoomClick">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="location_desc" label="位置" />
          <el-table-column prop="seat_total" label="座位数" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="roomStatusTagType(row.status)">{{ roomStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210">
            <template #default="{ row }">
              <el-button size="small" @click.stop="openRoomEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click.stop="deleteRoom(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager-row">
          <el-pagination
            :current-page="roomPage"
            :page-size="PAGE_SIZE"
            layout="prev, pager, next, total"
            :total="roomTotal"
            @current-change="loadRooms"
          />
        </div>
      </section>

      <section v-show="activeSection === 'seats'" class="panel">
        <div class="toolbar">
          <el-select v-model="currentRoomId" placeholder="请选择房间" style="width: 240px" @change="onRoomSelectChange">
            <el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" />
          </el-select>
          <el-button type="primary" :disabled="!currentRoomId" @click="openSeatCreate">新增座位</el-button>
        </div>
        <el-table :data="seats" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="seat_no" label="座位号" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="seatTypeTagType(row.seat_type)">{{ seatTypeText(row.seat_type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="seatStatusTagType(row.status)">{{ seatStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210">
            <template #default="{ row }">
              <el-button size="small" @click="openSeatEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteSeat(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager-row">
          <el-pagination
            :current-page="seatPage"
            :page-size="PAGE_SIZE"
            layout="prev, pager, next, total"
            :total="seatTotal"
            @current-change="loadSeatsAndRule"
          />
        </div>
      </section>

      <section v-show="activeSection === 'rules'" class="panel">
        <div class="toolbar">
          <el-select v-model="currentRoomId" placeholder="请选择房间" style="width: 240px" @change="onRoomSelectChange">
            <el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" />
          </el-select>
          <el-button type="primary" :disabled="!currentRoomId" @click="saveRule">保存规则</el-button>
        </div>
        <el-form label-width="140px" class="rule-form">
          <el-form-item label="每天最大预约次数"><el-input v-model.number="rule.max_bookings_per_day" /></el-form-item>
          <el-form-item label="单次最大时长"><el-input v-model.number="rule.max_hours_per_booking" /></el-form-item>
          <el-form-item label="签到宽限分钟"><el-input v-model.number="rule.checkin_grace_minutes" /></el-form-item>
          <el-form-item label="临时离开分钟"><el-input v-model.number="rule.temp_leave_minutes" /></el-form-item>
        </el-form>
      </section>

      <section v-show="activeSection === 'users'" class="panel">
        <div class="toolbar">
          <el-button type="primary" @click="loadUsers">刷新用户</el-button>
        </div>
        <el-table :data="users" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 9 ? 'danger' : 'info'">{{ row.role === 9 ? "管理员" : "用户" }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="账号状态" width="110">
            <template #default="{ row }">
              <el-tag :type="userStatusTagType(row.status)">{{ userStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="violation_count" label="违规次数" width="90" />
          <el-table-column label="操作" width="320">
            <template #default="{ row }">
              <el-button size="small" @click="openUserEdit(row)">编辑</el-button>
              <el-button
                size="small"
                :type="Number(row.status) === 1 ? 'warning' : 'success'"
                @click="toggleUserStatus(row)"
              >
                {{ Number(row.status) === 1 ? "禁用" : "启用" }}
              </el-button>
              <el-button size="small" @click="resetViolations(row)">清空违规</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager-row">
          <el-pagination
            :current-page="userPage"
            :page-size="PAGE_SIZE"
            layout="prev, pager, next, total"
            :total="userTotal"
            @current-change="loadUsers"
          />
        </div>
      </section>

      <section v-show="activeSection === 'reports'" class="panel">
        <el-table :data="reports" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="reporter_phone" label="举报人" width="130" />
          <el-table-column label="现场图片" width="120">
            <template #default="{ row }">
              <a v-if="row.image_url" :href="row.image_url" target="_blank" rel="noreferrer" class="report-image-link">
                <img :src="row.image_url" alt="举报图片" class="report-image-thumb" />
              </a>
              <span v-else>无</span>
            </template>
          </el-table-column>
          <el-table-column prop="description_text" label="描述" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="reportStatusTagType(row.status)">{{ reportStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="handleReport(row.id, 1)">属实</el-button>
              <el-button size="small" @click="handleReport(row.id, 2)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager-row">
          <el-pagination
            :current-page="reportPage"
            :page-size="PAGE_SIZE"
            layout="prev, pager, next, total"
            :total="reportTotal"
            @current-change="loadReports"
          />
        </div>
      </section>
    </main>

    <el-dialog v-model="roomDialogVisible" :title="roomForm.id ? '编辑房间' : '新增房间'" width="520px">
      <el-form :model="roomForm" label-width="90px">
        <el-form-item label="房间名称">
          <el-input v-model="roomForm.name" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="位置描述">
          <el-input v-model="roomForm.locationDesc" placeholder="例如 3号楼 5F A区" />
        </el-form-item>
        <el-form-item label="设施">
          <el-input v-model="roomForm.facilities" placeholder="例如 饮水、打印、插座" />
        </el-form-item>
        <el-form-item label="座位总数">
          <el-input-number v-model="roomForm.seatTotal" :min="0" :max="5000" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roomForm.status" style="width: 160px">
            <el-option :value="1" label="开放" />
            <el-option :value="0" label="关闭" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoom">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="seatDialogVisible" :title="seatForm.id ? '编辑座位' : '新增座位'" width="500px">
      <el-form :model="seatForm" label-width="100px">
        <el-form-item label="座位号">
          <el-input v-model="seatForm.seatNo" placeholder="例如 A-01" />
        </el-form-item>
        <el-form-item label="座位类型">
          <el-select v-model="seatForm.seatType" style="width: 160px">
            <el-option :value="1" label="普通座" />
            <el-option :value="2" label="静音区" />
            <el-option :value="3" label="带插座" />
          </el-select>
        </el-form-item>
        <el-form-item label="X坐标">
          <el-input-number v-model="seatForm.xPos" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="Y坐标">
          <el-input-number v-model="seatForm.yPos" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="seatForm.status" style="width: 160px">
            <el-option :value="1" label="可用" />
            <el-option :value="0" label="关闭" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="seatDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSeat">{{ seatForm.id ? "保存" : "确定" }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userDialogVisible" title="编辑用户" width="520px">
      <el-form :model="userForm" label-width="100px">
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 180px">
            <el-option :value="1" label="用户" />
            <el-option :value="9" label="管理员" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="userForm.status" style="width: 180px">
            <el-option :value="1" label="正常" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规次数">
          <el-input-number v-model="userForm.violationCount" :min="0" :max="99" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import * as echarts from "echarts";
import { apiDelete, apiGet, apiPost } from "../../api/http";

type SectionKey = "dashboard" | "rooms" | "seats" | "rules" | "users" | "reports";

const menus: Array<{ key: SectionKey; label: string }> = [
  { key: "dashboard", label: "首页概览" },
  { key: "rooms", label: "自习室管理" },
  { key: "seats", label: "座位管理" },
  { key: "rules", label: "预约规则" },
  { key: "users", label: "用户管理" },
  { key: "reports", label: "举报处理" }
];
const activeSection = ref<SectionKey>("dashboard");
const currentTitle = computed(() => menus.find((m) => m.key === activeSection.value)?.label || "管理台");

const rooms = ref<any[]>([]);
const seats = ref<any[]>([]);
const reports = ref<any[]>([]);
const users = ref<any[]>([]);
const currentRoomId = ref<number | null>(null);
const currentRoomName = ref("");
const rule = reactive<Record<string, any>>({});
const dashboard = reactive<Record<string, any>>({});
const from = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10));
const to = ref(new Date().toISOString().slice(0, 10));
const roomDialogVisible = ref(false);
const seatDialogVisible = ref(false);
const userDialogVisible = ref(false);

const roomForm = reactive({
  id: null as number | null,
  name: "",
  locationDesc: "",
  facilities: "",
  seatTotal: 0,
  status: 1
});
const seatForm = reactive({
  id: null as number | null,
  seatNo: "",
  seatType: 1,
  xPos: 1,
  yPos: 1,
  status: 1
});
const userForm = reactive({
  id: null as number | null,
  phone: "",
  nickname: "",
  role: 1,
  status: 1,
  violationCount: 0
});

const trendChartRef = ref<HTMLDivElement | null>(null);
const seatTypeChartRef = ref<HTMLDivElement | null>(null);
let trendChart: echarts.ECharts | null = null;
let seatTypeChart: echarts.ECharts | null = null;
const PAGE_SIZE = 8;
const roomPage = ref(1);
const seatPage = ref(1);
const userPage = ref(1);
const reportPage = ref(1);
const roomTotal = ref(0);
const seatTotal = ref(0);
const userTotal = ref(0);
const reportTotal = ref(0);

const switchSection = (key: SectionKey) => {
  activeSection.value = key;
};

const loadRooms = async (page = roomPage.value) => {
  roomPage.value = page;
  const data = await apiGet<{ items: any[]; total: number }>("/admin/rooms", {
    page: roomPage.value,
    pageSize: PAGE_SIZE
  });
  rooms.value = data.items || [];
  roomTotal.value = Number(data.total || 0);
  if (!currentRoomId.value && rooms.value.length > 0) {
    currentRoomId.value = rooms.value[0].id;
    currentRoomName.value = rooms.value[0].name || `ID:${rooms.value[0].id}`;
    await loadSeatsAndRule(1);
  }
  if (currentRoomId.value && !currentRoomName.value) {
    currentRoomName.value = `ID:${currentRoomId.value}`;
  }
};

const onRoomClick = async (row: any) => {
  currentRoomId.value = row.id;
  currentRoomName.value = row.name || `ID:${row.id}`;
  await loadSeatsAndRule(1);
};

const onRoomSelectChange = async (roomId: number) => {
  const target = rooms.value.find((item) => Number(item.id) === Number(roomId));
  currentRoomName.value = target?.name || `ID:${roomId}`;
  await loadSeatsAndRule(1);
};

const loadSeatsAndRule = async (page = seatPage.value) => {
  if (!currentRoomId.value) return;
  seatPage.value = page;
  const data = await apiGet<{ items: any[]; total: number }>(`/admin/rooms/${currentRoomId.value}/seats`, {
    page: seatPage.value,
    pageSize: PAGE_SIZE
  });
  seats.value = data.items || [];
  seatTotal.value = Number(data.total || 0);
  Object.assign(rule, await apiGet(`/admin/rooms/${currentRoomId.value}/rule`));
};

const openRoomCreate = () => {
  roomForm.id = null;
  roomForm.name = "";
  roomForm.locationDesc = "";
  roomForm.facilities = "";
  roomForm.seatTotal = 0;
  roomForm.status = 1;
  roomDialogVisible.value = true;
};

const openRoomEdit = (row: any) => {
  roomForm.id = row.id;
  roomForm.name = row.name || "";
  roomForm.locationDesc = row.location_desc || "";
  roomForm.facilities = row.facilities || "";
  roomForm.seatTotal = Number(row.seat_total || 0);
  roomForm.status = Number(row.status ?? 1);
  roomDialogVisible.value = true;
};

const saveRoom = async () => {
  if (!roomForm.name.trim()) {
    ElMessage.warning("请输入房间名称");
    return;
  }
  if (!roomForm.locationDesc.trim()) {
    ElMessage.warning("请输入位置描述");
    return;
  }
  await apiPost("/admin/rooms", {
    id: roomForm.id,
    name: roomForm.name.trim(),
    locationDesc: roomForm.locationDesc.trim(),
    facilities: roomForm.facilities.trim(),
    seatTotal: roomForm.seatTotal,
    status: roomForm.status
  });
  ElMessage.success(roomForm.id ? "房间已更新" : "新增房间成功");
  roomDialogVisible.value = false;
  await loadRooms();
};

const openSeatCreate = () => {
  if (!currentRoomId.value) {
    ElMessage.warning("请先选择房间");
    return;
  }
  seatForm.id = null;
  seatForm.seatNo = "";
  seatForm.seatType = 1;
  seatForm.xPos = 1;
  seatForm.yPos = 1;
  seatForm.status = 1;
  seatDialogVisible.value = true;
};

const openSeatEdit = (row: any) => {
  seatForm.id = row.id;
  seatForm.seatNo = row.seat_no || "";
  seatForm.seatType = Number(row.seat_type || 1);
  seatForm.xPos = Number(row.x_pos || 1);
  seatForm.yPos = Number(row.y_pos || 1);
  seatForm.status = Number(row.status ?? 1);
  seatDialogVisible.value = true;
};

const saveSeat = async () => {
  if (!currentRoomId.value) {
    ElMessage.warning("请先选择房间");
    return;
  }
  if (!seatForm.seatNo.trim()) {
    ElMessage.warning("请输入座位号");
    return;
  }
  await apiPost(`/admin/rooms/${currentRoomId.value}/seats`, {
    id: seatForm.id,
    seatNo: seatForm.seatNo.trim(),
    seatType: seatForm.seatType,
    xPos: seatForm.xPos,
    yPos: seatForm.yPos,
    status: seatForm.status
  });
  ElMessage.success(seatForm.id ? "座位已更新" : "新增座位成功");
  seatDialogVisible.value = false;
  await loadSeatsAndRule();
};

const deleteRoom = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除房间「${row.name}」吗？`, "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
    await apiDelete(`/admin/rooms/${row.id}`);
    ElMessage.success("房间已删除");
    if (Number(currentRoomId.value) === Number(row.id)) {
      currentRoomId.value = null;
      currentRoomName.value = "";
      seats.value = [];
    }
    await loadRooms();
  } catch (err: any) {
    if (err !== "cancel" && err !== "close") {
      ElMessage.error(err?.response?.data?.message || "删除房间失败");
    }
  }
};

const deleteSeat = async (row: any) => {
  if (!currentRoomId.value) return;
  try {
    await ElMessageBox.confirm(`确定删除座位「${row.seat_no}」吗？`, "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
    await apiDelete(`/admin/rooms/${currentRoomId.value}/seats/${row.id}`);
    ElMessage.success("座位已删除");
    await loadSeatsAndRule();
  } catch (err: any) {
    if (err !== "cancel" && err !== "close") {
      ElMessage.error(err?.response?.data?.message || "删除座位失败");
    }
  }
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

const loadReports = async (page = reportPage.value) => {
  reportPage.value = page;
  const data = await apiGet<{ items: any[]; total: number }>("/admin/reports", {
    page: reportPage.value,
    pageSize: PAGE_SIZE
  });
  reports.value = data.items || [];
  reportTotal.value = Number(data.total || 0);
};

const loadUsers = async (page = userPage.value) => {
  userPage.value = page;
  const data = await apiGet<{ items: any[]; total: number }>("/admin/users", {
    page: userPage.value,
    pageSize: PAGE_SIZE
  });
  users.value = data.items || [];
  userTotal.value = Number(data.total || 0);
};

const openUserEdit = (row: any) => {
  userForm.id = row.id;
  userForm.phone = row.phone || "";
  userForm.nickname = row.nickname || "";
  userForm.role = Number(row.role ?? 1);
  userForm.status = Number(row.status ?? 1);
  userForm.violationCount = Number(row.violation_count ?? 0);
  userDialogVisible.value = true;
};

const saveUser = async () => {
  if (!userForm.id) return;
  if (!userForm.phone.trim()) {
    ElMessage.warning("请输入手机号");
    return;
  }
  if (!userForm.nickname.trim()) {
    ElMessage.warning("请输入昵称");
    return;
  }
  await apiPost(`/admin/users/${userForm.id}`, {
    phone: userForm.phone.trim(),
    nickname: userForm.nickname.trim(),
    role: userForm.role,
    status: userForm.status,
    violationCount: userForm.violationCount
  });
  userDialogVisible.value = false;
  ElMessage.success("用户信息已更新");
  await loadUsers();
};

const handleReport = async (id: number, status: number) => {
  await apiPost(`/admin/reports/${id}/handle`, { status, note: "后台处理" });
  ElMessage.success("处理完成");
  await loadReports();
};

const toggleUserStatus = async (row: any) => {
  const nextStatus = Number(row.status) === 1 ? 0 : 1;
  await apiPost(`/admin/users/${row.id}/status`, { status: nextStatus });
  ElMessage.success(nextStatus === 1 ? "用户已启用" : "用户已禁用");
  await loadUsers();
};

const resetViolations = async (row: any) => {
  await apiPost(`/admin/users/${row.id}/reset-violations`);
  ElMessage.success("违规记录已清空");
  await loadUsers();
};

const loadDashboard = async () => {
  Object.assign(dashboard, await apiGet("/admin/dashboard", { from: from.value, to: to.value }));
  await nextTick();
  renderCharts();
};

const renderCharts = () => {
  if (!trendChartRef.value || !seatTypeChartRef.value) return;

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value);
  }
  if (!seatTypeChart) {
    seatTypeChart = echarts.init(seatTypeChartRef.value);
  }

  const monthly = (dashboard.monthlyTrend || []) as Array<{ ym: string; total: number }>;
  const hotSeat = (dashboard.hotSeatType || []) as Array<{ seat_type: number; c: number }>;

  trendChart.setOption({
    color: ["#0f766e"],
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: monthly.map((i) => i.ym).reverse() },
    yAxis: { type: "value" },
    series: [{ type: "line", smooth: true, data: monthly.map((i) => Number(i.total || 0)).reverse() }]
  });

  seatTypeChart.setOption({
    color: ["#0f766e", "#14b8a6", "#0ea5e9", "#64748b"],
    tooltip: { trigger: "item" },
    series: [
      {
        type: "pie",
        radius: ["35%", "68%"],
        data: hotSeat.map((i) => ({ name: seatTypeText(Number(i.seat_type)), value: Number(i.c || 0) }))
      }
    ]
  });
};

function roomStatusText(status: number) {
  const m: Record<number, string> = { 1: "开放", 0: "关闭" };
  return m[Number(status)] || `未知(${status})`;
}

function roomStatusTagType(status: number): "success" | "info" | "warning" {
  if (Number(status) === 1) return "success";
  if (Number(status) === 0) return "info";
  return "warning";
}

function seatTypeText(seatType: number) {
  const m: Record<number, string> = { 1: "普通座", 2: "静音区", 3: "带插座" };
  return m[Number(seatType)] || `类型${seatType}`;
}

function seatTypeTagType(seatType: number): "info" | "warning" | "primary" {
  if (Number(seatType) === 1) return "info";
  if (Number(seatType) === 2) return "warning";
  return "primary";
}

function seatStatusText(status: number) {
  const m: Record<number, string> = { 1: "可用", 0: "关闭" };
  return m[Number(status)] || `未知(${status})`;
}

function seatStatusTagType(status: number): "success" | "info" | "warning" {
  if (Number(status) === 1) return "success";
  if (Number(status) === 0) return "info";
  return "warning";
}

function reportStatusText(status: number) {
  const m: Record<number, string> = { 0: "待处理", 1: "属实", 2: "驳回" };
  return m[Number(status)] || `未知(${status})`;
}

function reportStatusTagType(status: number): "warning" | "success" | "info" {
  if (Number(status) === 0) return "warning";
  if (Number(status) === 1) return "success";
  return "info";
}

function userStatusText(status: number) {
  return Number(status) === 1 ? "正常" : "禁用";
}

function userStatusTagType(status: number): "success" | "danger" {
  return Number(status) === 1 ? "success" : "danger";
}

watch(
  () => activeSection.value,
  async (key) => {
    if (key === "dashboard") {
      await nextTick();
      renderCharts();
    }
  }
);

const onResize = () => {
  trendChart?.resize();
  seatTypeChart?.resize();
};

onBeforeUnmount(() => {
  window.removeEventListener("resize", onResize);
  trendChart?.dispose();
  seatTypeChart?.dispose();
});

const init = async () => {
  window.addEventListener("resize", onResize);
  await loadRooms();
  await loadUsers();
  await loadReports();
  await loadDashboard();
};
init();
</script>

<style scoped>
.admin-shell {
  background: #f6faf9;
  min-height: calc(100vh - 64px);
  display: grid;
  grid-template-columns: 220px 1fr;
  margin: -24px;
  width: calc(100% + 48px);
  overflow: hidden;
}
.sidebar {
  background: linear-gradient(180deg, #0f766e, #0d9488);
  padding: 12px;
  min-height: calc(100vh - 64px);
}
.menu-btn {
  width: 100%;
  border: none;
  background: transparent;
  text-align: left;
  color: #cffafe;
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 6px;
  font-size: 14px;
}
.menu-btn.active {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  font-weight: 600;
}
.content {
  padding: 16px;
  overflow: auto;
}
.content-head h3 {
  margin: 0 0 12px;
  font-size: 20px;
  color: #0f172a;
}
.panel {
  background: #ffffff;
  border: 1px solid #e2ece8;
  border-radius: 12px;
  padding: 14px;
}
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.sub-text {
  margin: 0 0 10px;
  color: #475569;
}
.rule-form {
  max-width: 520px;
}
.pager-row {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.stat-card {
  border: 1px solid #dbe7e4;
  border-radius: 10px;
  padding: 12px;
  background: linear-gradient(180deg, #ffffff, #f8fffd);
}
.stat-card span {
  color: #64748b;
  font-size: 12px;
}
.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #0f172a;
}
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.chart-card {
  border: 1px solid #dbe7e4;
  border-radius: 10px;
  padding: 10px;
}
.chart-card h4 {
  margin: 0 0 8px;
  color: #334155;
  font-size: 14px;
}
.chart {
  height: 280px;
}
.report-image-link {
  display: inline-block;
}
.report-image-thumb {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #dbe7e4;
}
</style>
