cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
$hds = 16384, 32768
$ensembleSizes = 10, 20 
$aggregateByMajorityVote = $true, $false
$minEpochs = 10, 30

#to set value, refer to the config string format below. also be sure how the numbers are formatted
$ignoredConfigs = @()
$resumedLearnerId = 0

#resume experiment for learner 
IF($resumedLearnerId -ne 0){
    java -jar thesis-boostedEnsemble-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId
}

$hds | foreach{
    $hd = $_

    $ensembleSizes | foreach{
        $es = $_

        $aggregateByMajorityVote | foreach{
            $aggrPref = $_

             $minEpochs | foreach{
                $minEpoch = $_
                $conf = "$($hd) - $($es) - $($aggrPref) - $($minEpoch)" #config string format

                IF(-Not ($conf -in $ignoredConfigs)){       
                    java -jar thesis-boostedEnsemble-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -hd $hd -es $es -mv $aggrPref -me $minEpoch
                }
                Copy-Item "../simulationResults/ensembleBoostingData.tex" $env:ThesisExpTex -Force
                Copy-Item "../simulationResults/PLTEnsembleBoosted" $env:ThesisExpResults -Force -Recurse
            }
        }
    }
}

<#
All config strings (in order), while copying ignore the serial number at the begining:
1 16384 - 10 - True - 10
2 16384 - 10 - True - 30
3 16384 - 10 - False - 10
4 16384 - 10 - False - 30
5 16384 - 20 - True - 10
6 16384 - 20 - True - 30
7 16384 - 20 - False - 10
8 16384 - 20 - False - 30
9 32768 - 10 - True - 10
10 32768 - 10 - True - 30
11 32768 - 10 - False - 10
12 32768 - 10 - False - 30
13 32768 - 20 - True - 10
14 32768 - 20 - True - 30
15 32768 - 20 - False - 10
16 32768 - 20 - False - 30
#>