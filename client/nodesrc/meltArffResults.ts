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

function getWeightingStrategy(config) {
  return config.isToAggregateByLambdaCW ? 'LCW' : (config.isToAggregateByMajorityVote ? 'MV' : 'FM');
}

function getLearnersConfig(): Map<string, string> {
  const configDirName = "./nodesrc/learnerConfigurations/"
  const learnerConfigMap = new Map<string, string>();

  fs.readdirSync(configDirName)
    .filter(file => file.endsWith("json"))
    .forEach(file => {
      const learnerId = file.replace(".json", "");
      const content = fs.readFileSync(`${configDirName}${file}`, 'utf8');
      const config = JSON.parse(content)[1];
      learnerConfigMap.set(learnerId,
        `${config.individualPLTConfiguration[1].hd}-${config.ensembleSize}-${config.minEpochs}-${getWeightingStrategy(config)}`);
    });
  return learnerConfigMap;
}

function readArffFileData(arffFilePath: string): Result[] {
  const content = fs.readFileSync(arffFilePath, 'utf8');
  return arff.parse(content).data;
}

/**
 * Sorts by Learner, Category, CreatedOn.
 * @note Array order: RecordId, Learner, Category, CreatedOn, Fm, CumFm, Regret, CumRegret
 * @param {any[]} a 
 * @param {any[]} b 
 * @returns 
 */
function sortFunction(a: any[], b: any[]) {
  const aLearner: string = a[1];
  const bLearner: string = b[1];

  if (aLearner !== bLearner) return aLearner.localeCompare(bLearner);

  const aCategory: string = a[2];
  const bCategory: string = b[2];

  if (aCategory !== bCategory) return aCategory.localeCompare(bCategory);

  const aCreatedOn = moment(a[3]);
  const bCreatedOn = moment(b[3]);
  return aCreatedOn.isBefore(bCreatedOn) ? -1 : 1;
}

function meltResult(result: Result[], learnerConfig: string | undefined) {

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

function writeCsv(data: any[][], baseDir: string) {
  const fileName = `${baseDir}allResults.csv`;
  fs.writeFileSync(fileName, "RecordId,Learner,Category,CreatedOn,Fm,CumFm,Regret,CumRegret\n");
  fs.appendFileSync(fileName, data.map(item => item.join(",")).join("\n"));
}

const learnerConfigMap = getLearnersConfig();

function meltArffData(baseDir: string) {
  let allResults: any[][] = [];

  console.log(`[${new Date()}] processing ${baseDir}`);

  fs.readdirSync(baseDir)
    .filter(file => file.endsWith("arff"))
    .forEach(file => {
      console.log(`[${new Date()}] processing ${file}`);
      const learnerId = file.replace(".arff", "");
      const learnerConfig = learnerConfigMap.get(learnerId);

      const result = readArffFileData(`${baseDir}${file}`);
      allResults = allResults.concat(meltResult(result, learnerConfig));
      console.log(`[${new Date()}] processed ${file}, all data count: ${allResults.length}`);
    });
  writeCsv(allResults, baseDir);
  console.log(`[${new Date()}] all data from ${baseDir} is melted and written to csv`);
}

// const d = readArffFileData("./nodesrc/arffResults/boostedEnsemble/3c75f5f6-54c0-44e1-b29a-5a2724e4d6c4.arff")[0].QuestionId
// console.log(typeof d, d);
// console.log(moment(d).format("YYYY-MM-DD HH:mm:ss.SSS"));

meltArffData("./nodesrc/arffResults/boostedEnsemble/");
meltArffData("./nodesrc/arffResults/boostedEnsembleWithThreshold/");
