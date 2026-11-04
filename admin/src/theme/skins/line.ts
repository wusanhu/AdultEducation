import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

/**
 * 皮肤定义：一套皮肤 = Ant Design Vue 主题令牌 + 控制台自身的 CSS 变量。
 * 皮肤只通过代码/构建变量选择，不提供任何界面切换入口。
 */
export interface AdminSkin {
  key: string
  name: string
  theme: ThemeConfig
  vars: Record<string, string>
}

const FONT_FAMILY = "'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif"

export const lineSkin: AdminSkin = {
  key: 'line',
  name: '线性白底皮肤',
  theme: {
    token: {
      colorPrimary: '#1f6feb',
      colorInfo: '#1f6feb',
      colorSuccess: '#16a34a',
      colorWarning: '#d97706',
      colorError: '#dc2626',
      colorText: '#0f172a',
      colorTextSecondary: '#475569',
      colorTextTertiary: '#94a3b8',
      colorBorder: '#e3eaf4',
      colorBorderSecondary: '#eef2f7',
      colorSplit: '#eef2f7',
      colorBgLayout: '#ffffff',
      colorBgContainer: '#ffffff',
      colorBgElevated: '#ffffff',
      colorFillAlter: '#fafbfd',
      borderRadius: 8,
      borderRadiusLG: 12,
      borderRadiusSM: 6,
      fontFamily: FONT_FAMILY,
      // 平面化：Ant Design 自身的阴影令牌统一置空
      boxShadow: 'none',
      boxShadowSecondary: 'none',
      boxShadowTertiary: 'none'
    }
  },
  vars: {
    '--admin-shell-bg': '#fafcff',
    '--admin-canvas-bg': '#ffffff',
    '--admin-canvas-border': '#e6edf7',
    '--admin-line': '#eef2f7',
    '--admin-line-strong': '#e3eaf4',
    '--admin-primary': '#1f6feb',
    '--admin-text': '#0f172a',
    '--admin-text-muted': '#64748b',
    '--admin-radius-lg': '16px',
    '--admin-radius-md': '12px',
    '--admin-radius-sm': '8px'
  }
}
