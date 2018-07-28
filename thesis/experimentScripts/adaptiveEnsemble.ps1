cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
$ageFunctions = "NumberOfLabelsBased", "NumberTrainingInstancesBased"
$epsilons = 0.000001, 0.001 
$nMins = 2, 50
$retainmentFractions =  0.1, 0.2

#to set value, refer to the config string format below. also be sure how the numbers are formatted
$ignoredConfigs = @()
$resumedLearnerId = 0

#resume experiment for learner 
IF($resumedLearnerId -ne 0){
    java -jar thesis-adaptiveEnsemble-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId
}

$ageFunctions | foreach{
    $af = $_

    $epsilons | foreach{
        $epsilon = $_

        $nMins | foreach{
            $nMin = $_

             $retainmentFractions | foreach{
                $retainmentFraction = $_
                $conf = "$($af) - $($epsilon) - $($nMin) - $($retainmentFraction)" #config string format

                IF(-Not ($conf -in $ignoredConfigs)){                       
                    java -jar thesis-adaptiveEnsemble-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -af $af -ep $epsilon -nm $nMin -ret $retainmentFraction
                }
                Copy-Item "../simulationResults/adaptiveEnsembleData.tex" $env:ThesisExpTex -Force
                Copy-Item "../simulationResults/PLTAdaptiveEnsemble" $env:ThesisExpResults -Force -Recurse
            }
        }
    }
}

<#
All config strings (in order), while copying ignore the serial number at the begining:
1 NumberOfLabelsBased - 1E-06 - 2 - 0.1
2 NumberOfLabelsBased - 1E-06 - 2 - 0.2
3 NumberOfLabelsBased - 1E-06 - 50 - 0.1
4 NumberOfLabelsBased - 1E-06 - 50 - 0.2
5 NumberOfLabelsBased - 0.001 - 2 - 0.1
6 NumberOfLabelsBased - 0.001 - 2 - 0.2
7 NumberOfLabelsBased - 0.001 - 50 - 0.1
8 NumberOfLabelsBased - 0.001 - 50 - 0.2
9 NumberTrainingInstancesBased - 1E-06 - 2 - 0.1
10 NumberTrainingInstancesBased - 1E-06 - 2 - 0.2
11 NumberTrainingInstancesBased - 1E-06 - 50 - 0.1
12 NumberTrainingInstancesBased - 1E-06 - 50 - 0.2
13 NumberTrainingInstancesBased - 0.001 - 2 - 0.1
14 NumberTrainingInstancesBased - 0.001 - 2 - 0.2
15 NumberTrainingInstancesBased - 0.001 - 50 - 0.1
16 NumberTrainingInstancesBased - 0.001 - 50 - 0.2
#>