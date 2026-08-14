// 烧烤菜品网络图片工具
// 采用 LoremFlickr 按关键词拉取真实美食照片（来自网络 Flickr），
// 通过确定性的 lock 参数保证同一菜品每次都拿到同一张图，避免渲染时图片闪烁。
// 相比写死的 Unsplash 直链，这种方式不会因图片 ID 失效而出现 404 / 加载不出来的情况。

const KEYWORDS = {
  '羊肉串': 'lamb,skewer',
  '牛肉串': 'beef,skewer',
  '鸡翅': 'chicken,wing',
  '五花肉': 'pork,belly',
  '烤腰子': 'meat,barbecue',
  '烤生蚝': 'oyster,grilled',
  '烤鱿鱼': 'squid,grilled',
  '烤大虾': 'shrimp,grilled',
  '烤馒头片': 'bread,toast',
  '烤韭菜': 'vegetable,grilled',
  '烤茄子': 'eggplant,grilled',
  '啤酒': 'beer',
  '王老吉': 'tea,drink',
  '酸梅汤': 'plum,drink',
}

// 根据菜名生成稳定的 lock 值（同名永远返回同一张图）
function lockFor(name) {
  let h = 0
  for (let i = 0; i < name.length; i++) {
    h = (h * 31 + name.charCodeAt(i)) >>> 0
  }
  return (h % 1000) + 1
}

// 返回某道菜的网络图片地址；未知菜品回退到烧烤通用图
export function getFoodImage(name) {
  const kw = KEYWORDS[name] || 'barbecue,meat'
  return `https://loremflickr.com/320/320/${kw}?lock=${lockFor(name)}`
}
