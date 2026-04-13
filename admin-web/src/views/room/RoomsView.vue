<template>
  <section class="card">
    <h2>自习室信息与选座预约</h2>
    <div class="toolbar">
      <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" />
      <el-select v-model="slotCode" style="width: 120px">
        <el-option label="早" :value="1" />
        <el-option label="中" :value="2" />
        <el-option label="晚" :value="3" />
      </el-select>
      <el-button @click="loadRooms">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card v-for="room in rooms" :key="room.id" class="room-card" @click="chooseRoom(room.id)">
          <h3>{{ room.name }}</h3>
          <p>{{ room.location_desc }}</p>
          <p>设施: {{ room.facilities }}</p>
          <p>评分: {{ room.avg_score }}</p>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card v-if="currentRoomId">
          <template #header>座位状态（绿色空闲 / 红色已订 / 黄色离开 / 灰色关闭）</template>
          <div class="seat-grid">
            <button
              v-for="seat in seatSlots"
              :key="seat.seat_id"
              class="seat-btn"
              :class="seatClass(seat.slot_status)"
              @click="book(seat)"
              :disabled="seat.slot_status !== 0"
            >
              {{ seat.seat_no }}
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { apiGet, apiPost } from "../../api/http";

type Room = { id: number; name: string; location_desc: string; facilities: string; avg_score: number };
type SeatSlot = { seat_id: number; seat_no: string; slot_status: number };

const date = ref(new Date().toISOString().slice(0, 10));
const slotCode = ref(1);
const rooms = ref<Room[]>([]);
const currentRoomId = ref<number | null>(null);
const seatSlots = ref<SeatSlot[]>([]);

const loadRooms = async () => {
  try {
    rooms.value = await apiGet<Room[]>("/rooms");
    if (!currentRoomId.value && rooms.value.length > 0) {
      await chooseRoom(rooms.value[0].id);
    } else if (currentRoomId.value) {
      await loadSlots();
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载自习室失败");
  }
};

const chooseRoom = async (roomId: number) => {
  currentRoomId.value = roomId;
  await loadSlots();
};

const loadSlots = async () => {
  if (!currentRoomId.value) return;
  try {
    seatSlots.value = await apiGet<SeatSlot[]>(`/rooms/${currentRoomId.value}/seat-slots`, {
      date: date.value,
      slotCode: slotCode.value
    });
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载座位失败");
  }
};

const book = async (seat: SeatSlot) => {
  if (!currentRoomId.value) return;
  try {
    const data = await apiPost<{ orderNo: string; voucherCode: string }>("/bookings", {
      roomId: currentRoomId.value,
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

const seatClass = (status: number) => {
  if (status === 0) return "free";
  if (status === 1) return "booked";
  if (status === 2) return "leave";
  return "closed";
};

loadRooms();
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.room-card {
  margin-bottom: 12px;
  cursor: pointer;
}
.seat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(56px, 1fr));
  gap: 10px;
}
.seat-btn {
  border: none;
  padding: 10px 6px;
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
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
</style>
