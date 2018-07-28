export class Utility {

  static getCumulativeSumArray(input: number[]): number[] {
    return input.reduce((acc: number[], item: number, index: number) => {
      acc.push((acc[index - 1] || 0) + item);
      return acc;
    }, []);
  }
}
