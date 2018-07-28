declare module "es6-promisify" {
  export default function promisify(original: (...args: any[]) => any, settings?: any): (...args: any[]) => Promise<any>
}
