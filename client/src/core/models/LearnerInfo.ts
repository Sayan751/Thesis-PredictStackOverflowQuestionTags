import { BaseModel } from './BaseModel';
import { FmeasuresInfo } from "./FmeasuresInfo";
import * as _ from "lodash";

export class LearnerInfo extends BaseModel {

  private _prequentialFmsCurrentInstance: FmeasuresInfo;
  private _postTrainingFmsCurrentInstance: FmeasuresInfo;

  public learnerClass: string;
  public learnerConfig: any;//LearnerInitConfiguration

  public macroFmeasure: number;
  public avgPrequentialFmeasures: FmeasuresInfo;
  public avgPostTrainingFmeasures: FmeasuresInfo;

  set prequentialFmsCurrentInstance(fmInfo: FmeasuresInfo) {
    if (fmInfo) {
      this._prequentialFmsCurrentInstance = fmInfo;
      if (this.cumulativeFmeasures) {
        this.cumulativeFmeasures.prequentialGeneralFm.push(_.last(this.cumulativeFmeasures.prequentialGeneralFm) + fmInfo.general);
        this.cumulativeFmeasures.prequentialTopkFm.push(_.last(this.cumulativeFmeasures.prequentialTopkFm) + fmInfo.topk);
      }
    }
  }
  get prequentialFmsCurrentInstance(): FmeasuresInfo {
    return this._prequentialFmsCurrentInstance;
  }

  set postTrainingFmsCurrentInstance(fmInfo: FmeasuresInfo) {
    if (fmInfo) {
      this._postTrainingFmsCurrentInstance = fmInfo;
      if (this.cumulativeFmeasures) {
        this.cumulativeFmeasures.postTrainingGeneralFm.push(_.last(this.cumulativeFmeasures.postTrainingGeneralFm) + fmInfo.general);
        this.cumulativeFmeasures.postTrainingTopkFm.push(_.last(this.cumulativeFmeasures.postTrainingTopkFm) + fmInfo.topk);
      }
    }
  }
  get postTrainingFmsCurrentInstance(): FmeasuresInfo {
    return this._postTrainingFmsCurrentInstance
  }

  public cumulativeFmeasures: CumulativeFmeasures | undefined;
}

export class CumulativeFmeasures {
  public prequentialGeneralFm: number[] = [];
  public prequentialTopkFm: number[] = [];
  public postTrainingGeneralFm: number[] = [];
  public postTrainingTopkFm: number[] = [];

  constructor(preqGen?: number[], preqTopk?: number[], postTrainGen?: number[], postTrainTopk?: number[]) {
    if (preqGen)
      this.prequentialGeneralFm = preqGen;
    if (preqTopk)
      this.prequentialTopkFm = preqTopk;
    if (postTrainGen)
      this.postTrainingGeneralFm = postTrainGen;
    if (postTrainTopk)
      this.postTrainingTopkFm = postTrainTopk;
  }
}
