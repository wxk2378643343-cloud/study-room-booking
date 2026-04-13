<template>
  <div class="mobile-layout">
    <main class="mobile-container" :class="{ 'with-tabbar': showTabBar }">
      <RouterView />
    </main>
    <MobileTabBar v-if="showTabBar" />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import MobileTabBar from "./components/MobileTabBar.vue";

const route = useRoute();

const showTabBar = computed(() => route.path !== "/login");
</script>
