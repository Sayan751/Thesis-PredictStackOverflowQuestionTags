export class FmeasureValueConverter {

  toView(fmeasure: number): number {
    if (!fmeasure) return 0;
    return Number(Math.round(Number(fmeasure * 100 + 'e' + 2)) + 'e-' + 2);
  }
}
