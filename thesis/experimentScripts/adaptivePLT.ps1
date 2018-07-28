cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
$hds = 16384, 32768
$alphas = 0.4, 0.5, 0.85, 1 
$preferNodeWithHighestProbability = $true, $false

#to set value, refer to the config string format below. also be sure how the numbers are formatted
$ignoredConfigs = @("16384 - 0.4 - True","16384 - 0.4 - False","16384 - 0.5 - True","16384 - 0.5 - False") 
$resumedLearnerId = "950ce8c2-4f97-4c03-8c05-360dc6ee0a20"

#resume experiment for learner 
IF($resumedLearnerId -ne 0){
    java -jar thesis-adaptivePlt-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId
}

$hds | foreach{
    $hd = $_

    $alphas | foreach{
        $alpha = $_

        $preferNodeWithHighestProbability | foreach{
            $pref = $_
            $conf = "$($hd) - $($alpha) - $($pref)" #config string format

            IF(-Not ($conf -in $ignoredConfigs)){           
                java -jar thesis-adaptivePlt-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -hd $hd -alpha $alpha -hp $pref
            }
            Copy-Item "../simulationResults/adaptivePLTData.tex" $env:ThesisExpTex -Force
            Copy-Item "../simulationResults/AdaptivePLT" $env:ThesisExpResults -Force -Recurse
        }
    }
}
<#
All config strings (in order), while copying ignore the serial number at the begining:
1 16384 - 0.4 - True
2 16384 - 0.4 - False
3 16384 - 0.5 - True
4 16384 - 0.5 - False
5 16384 - 0.85 - True
6 16384 - 0.85 - False
7 16384 - 1 - True
8 16384 - 1 - False
9 32768 - 0.4 - True
10 32768 - 0.4 - False
11 32768 - 0.5 - True
12 32768 - 0.5 - False
13 32768 - 0.85 - True
14 32768 - 0.85 - False
15 32768 - 1 - True
16 32768 - 1 - False
#>