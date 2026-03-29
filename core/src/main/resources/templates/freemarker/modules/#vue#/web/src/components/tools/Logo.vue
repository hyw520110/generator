<template>
  <div class="logo">
    <router-link :to="'/dashboard/workplace'">
      <img src="@/assets/logo.svg" alt="logo" class="logo-icon" />
      <h1 v-if="showTitle">{{ title }}</h1>
    </router-link>
  </div>
</template>

<script>
export default {
  name: 'Logo',
  props: {
    title: {
      type: String,
      default: 'Ant Design Pro',
      required: false
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false
    }
  }
}
</script>

<style scoped>
.logo-icon {
  width: 32px;
  height: 32px;
}
</style>
