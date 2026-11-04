import { lineSkin, type AdminSkin } from './skins/line'

/**
 * 皮肤注册表。新增皮肤时在此登记，即可通过代码选择，
 * 控制台界面不提供任何皮肤切换入口。
 */
const SKINS: Record<string, AdminSkin> = {
  [lineSkin.key]: lineSkin
}

function resolveSkin(): AdminSkin {
  const external = typeof window !== 'undefined' ? window.__ADMIN_SKIN__ : undefined
  const requested = external || import.meta.env.VITE_ADMIN_SKIN || ''
  return SKINS[requested] || lineSkin
}

export const activeSkin = resolveSkin()

/** 将皮肤变量写入根节点，供控制台自身样式读取 */
export function applySkinVariables(skin: AdminSkin = activeSkin): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  Object.entries(skin.vars).forEach(([name, value]) => root.style.setProperty(name, value))
  root.dataset.adminSkin = skin.key
}
