import * as fs from "fs";
import * as arff from "arff";
import * as moment from "moment";

interface Result {
  QuestionId: number;
  CreatedOn: Date;
  Fmeasure: number;
  TopkFmeasure: number;
  IsTest: 0 | 1;
  IsPrequential: 0 | 1
}

function readArffFileData(arffFilePath: string): Result[] {
  const content = fs.readFileSync(arffFilePath, 'utf8');
  return arff.parse(content).data;
}

function meltResult(result: Result[], learnerConfig: any) {

  const questionIds = Array
    .from(new Set(result.map((item: Result) => item.QuestionId)))
    .sort((a: number, b: number) => a - b);

  const preqGenData: any[][] = [], preqTopkData: any[][] = [],
    postTrainGenData: any[][] = [], postTrainTopkData: any[][] = [];

  let cumPreqFmGen = 0, cumPreqRegretGen = 0, cumPreqFmTopk = 0, cumPreqRegretTopk = 0,
    cumPostTrainFmGen = 0, cumPostTrainRegretGen = 0, cumPostTrainFmTopk = 0, cumPostTrainRegretTopk = 0;

  result
    .sort((item1: Result, item2: Result) => item1.QuestionId - item2.QuestionId)
    .forEach((item: Result) => {
      const recordId = questionIds.indexOf(item.QuestionId) + 1;
      if (!item.QuestionId || !recordId || (typeof item.QuestionId !== "number") || (typeof recordId !== "number")) throw item;
      const createdOn = moment(item.CreatedOn).format("YYYY-MM-DD HH:mm:ss.SSS");
      const regret = 1 - item.Fmeasure;
      const regretTopK = 1 - item.TopkFmeasure;


      if (item.IsPrequential == 1) {
        cumPreqFmGen += item.Fmeasure;
        cumPreqRegretGen += regret;
        cumPreqFmTopk += item.TopkFmeasure;
        cumPreqRegretTopk += regretTopK;

        //RecordId, Learner, Category, CreatedOn, Fm, CumFm, Regret, CumRegret
        preqGenData.push([recordId, learnerConfig, 'prequential general', createdOn,
          item.Fmeasure, cumPreqFmGen, regret, cumPreqRegretGen]);
        preqTopkData.push([recordId, learnerConfig, 'prequential topk', createdOn,
          item.TopkFmeasure, cumPreqFmTopk, regretTopK, cumPreqRegretTopk]);
      } else {
        cumPostTrainFmGen += item.Fmeasure;
        cumPostTrainRegretGen += regret;
        cumPostTrainFmTopk += item.TopkFmeasure;
        cumPostTrainRegretTopk += regretTopK;

        //RecordId, Learner, Category, CreatedOn, Fm, CumFm, Regret, CumRegret
        postTrainGenData.push([recordId, learnerConfig, 'post-training general', createdOn,
          item.Fmeasure, cumPostTrainFmGen, regret, cumPostTrainRegretGen]);
        postTrainTopkData.push([recordId, learnerConfig, 'post-training topk', createdOn,
          item.TopkFmeasure, cumPostTrainFmTopk, regretTopK, cumPostTrainRegretTopk]);
      }
    });
  return preqGenData.concat(preqTopkData).concat(postTrainGenData).concat(postTrainTopkData);
  // return retVal.sort(sortFunction);
}

function meltArffData(baseDir: string) {
  let allResults: any[][] = [];

  console.log(`[${moment().format()}] processing ${baseDir}`);

  const fileName = `${baseDir}allResults.csv`;
  fs.writeFileSync(fileName, "RecordId,Learner,Category,CreatedOn,Fm,CumFm,Regret,CumRegret\n");

  fs.readdirSync(baseDir)
    .filter(file => file.endsWith("arff"))
    .forEach(file => {
      console.log(`[${moment().format()}] processing ${file}`);
      const settings = file.replace(".arff", "");
      const settingsPart = settings.split("-");
      const learnerConfig = parseInt(settings.startsWith("plt") ? settingsPart[2] : settingsPart[4], 10);

      const result = readArffFileData(`${baseDir}${file}`);
      // console.log(meltResult(result, learnerConfig).filter(item => typeof item[1] === "string"));
      fs.appendFileSync(fileName, meltResult(result, learnerConfig).map(item => item.join(",")).join("\n")+"\n");
      console.log(`[${moment().format()}] processed ${file}, learnerConfig: ${learnerConfig}, length: ${result.length}`);
    });
  console.log(`[${moment().format()}] all data from ${baseDir} is melted and written to csv`);
}

meltArffData("./nodesrc/arffResults/PLT/");
meltArffData("./nodesrc/arffResults/adaptivePLT/");

// console.log(readArffFileData("./nodesrc/arffResults/PLT/pltArff-32768-2-25000-2017_06_19_09_06_34.arff").length)
