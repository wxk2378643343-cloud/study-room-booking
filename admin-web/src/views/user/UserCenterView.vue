<template>
  <section class="card">
    <h2>个人中心</h2>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <h3>个人资料</h3>
          <el-form label-width="80px">
            <el-form-item label="昵称">
              <el-input v-model="profile.nickname" />
            </el-form-item>
            <el-form-item label="常用自习室">
              <el-input v-model.number="profile.preferred_room_id" />
            </el-form-item>
            <el-button type="primary" @click="saveProfile">保存</el-button>
          </el-form>
          <p>违规次数: {{ profile.violation_count || 0 }}</p>
          <p>限制到期: {{ profile.booking_restricted_until || "无" }}</p>
          <p>积分余额: {{ points.points_balance || 0 }}</p>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-tabs>
          <el-tab-pane label="违规记录">
            <el-table :data="violations" border>
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="violation_type" label="类型" width="100" />
              <el-table-column prop="remark" label="说明" />
              <el-table-column prop="created_at" label="时间" width="180" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="积分流水">
            <el-table :data="pointLogs" border>
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="change_amount" label="变动" width="90" />
              <el-table-column prop="biz_type" label="类型" width="100" />
              <el-table-column prop="created_at" label="时间" width="180" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="优惠券">
            <el-table :data="coupons" border>
              <el-table-column prop="template_name" label="券名" />
              <el-table-column prop="status" label="状态" width="90" />
              <el-table-column prop="expire_at" label="过期时间" width="180" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="消息通知">
            <el-table :data="notifications" border>
              <el-table-column prop="title" label="标题" width="140" />
              <el-table-column prop="content" label="内容" />
              <el-table-column prop="is_read" label="已读" width="80" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" @click="markRead(row.id)">已读</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { apiGet, apiPost, apiPut } from "../../api/http";

const profile = reactive<Record<string, any>>({});
const points = reactive<Record<string, any>>({});
const violations = ref<any[]>([]);
const pointLogs = ref<any[]>([]);
const coupons = ref<any[]>([]);
const notifications = ref<any[]>([]);

const load = async () => {
  Object.assign(profile, await apiGet("/users/profile"));
  Object.assign(points, await apiGet("/users/points"));
  violations.value = await apiGet("/users/violations");
  pointLogs.value = await apiGet("/users/points/logs");
  coupons.value = await apiGet("/users/coupons");
  notifications.value = await apiGet("/users/notifications");
};

const saveProfile = async () => {
  await apiPut("/users/profile", {
    nickname: profile.nickname,
    preferredRoomId: profile.preferred_room_id
  });
  ElMessage.success("保存成功");
  await load();
};

const markRead = async (id: number) => {
  await apiPost(`/users/notifications/${id}/read`);
  await load();
};

load();
</script>
