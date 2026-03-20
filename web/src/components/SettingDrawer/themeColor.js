import { generate } from '@ant-design/colors'

export default {
  getAntdSerials (color) {
    const colorPalettes = generate(color)
    return colorPalettes
  },
  changeColor (newColor) {
    // Ant Design Vue 4.x 使用 CSS-in-JS，通过 CSS 变量设置主题色
    const root = document.documentElement
    root.style.setProperty('--ant-primary-color', newColor)
    
    // 生成色板
    const colorPalettes = this.getAntdSerials(newColor)
    colorPalettes.forEach((color, index) => {
      root.style.setProperty(`--ant-primary-${index + 1}`, color)
    })
    
    return Promise.resolve()
  }
}