import generate from '@ant-design/colors/lib/generate'

export default {
  getAntdSerials (color) {
    // colorPalette变换得到颜色值
    const colorPalettes = generate(color)
    return colorPalettes
  },
  changeColor (newColor) {
    // Vite 不支持 webpack-theme-color-replacer 动态换肤
    // 简化实现：仅更新 CSS 变量
    return new Promise((resolve) => {
      const colors = this.getAntdSerials(newColor)
      document.documentElement.style.setProperty('--primary-color', newColor)
      colors.forEach((c, i) => {
        document.documentElement.style.setProperty(`--primary-${i + 1}`, c)
      })
      console.log('主题色已更改为:', newColor)
      resolve()
    })
  }
}
