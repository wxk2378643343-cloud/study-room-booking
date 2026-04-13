<template>
  <section class="rooms-page">
    <div class="hero">
      <div>
        <h2>自习室</h2>
        <p>按位置、设施和评分快速选择</p>
      </div>
      <el-button class="refresh-btn" text @click="loadRooms">刷新</el-button>
    </div>

    <div class="room-list">
      <el-empty v-if="rooms.length === 0" description="暂无可用自习室" />
      <article v-for="room in rooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
        <div class="room-card__top">
          <h3>{{ room.name }}</h3>
          <span class="room-score">{{ formatScore(room.avg_score) }}分</span>
        </div>
        <p class="room-location">{{ room.location_desc }}</p>
        <div class="facility-list">
          <span v-for="item in splitFacilities(room.facilities)" :key="`${room.id}-${item}`" class="facility-tag">
            {{ item }}
          </span>
        </div>
        <div class="room-card__foot">
          <span>点击进入选座</span>
          <span class="arrow">></span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiGet } from "../../api/http";

type Room = { id: number; name: string; location_desc: string; facilities: string; avg_score: number };

const router = useRouter();
const rooms = ref<Room[]>([]);

const loadRooms = async () => {
  try {
    rooms.value = await apiGet<Room[]>("/rooms");
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || "加载自习室失败");
  }
};

const goDetail = (roomId: number) => {
  router.push(`/rooms/${roomId}`);
};

const splitFacilities = (facilities: string) => {
  return facilities
    .split(/[、,，/]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 4);
};

const formatScore = (score: number) => Number(score || 0).toFixed(1);

loadRooms();
</script>

<style scoped>
.rooms-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hero {
  background: linear-gradient(140deg, #0f766e, #0e8f85);
  border-radius: 16px;
  color: #f0fdfa;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.25);
}
.hero h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.hero p {
  margin: 0;
  font-size: 12px;
  color: rgba(240, 253, 250, 0.88);
}
.refresh-btn {
  color: #ffffff;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  padding: 7px 12px;
}
.room-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.room-card {
  cursor: pointer;
  background: #ffffff;
  border-radius: 14px;
  padding: 14px;
  border: 1px solid #dbe7e4;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}
.room-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.room-card__top h3 {
  margin: 0;
  font-size: 17px;
  color: #0f172a;
}
.room-score {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  background: #e7faf7;
  border-radius: 999px;
  padding: 3px 10px;
}
.room-location {
  margin: 8px 0 10px;
  color: #475569;
  font-size: 13px;
}
.facility-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.facility-tag {
  font-size: 12px;
  color: #0e7490;
  background: #ecfeff;
  border: 1px solid #bae6fd;
  border-radius: 999px;
  padding: 2px 8px;
}
.room-card__foot {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
}
.arrow {
  font-size: 14px;
}
</style>
