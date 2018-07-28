cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
$resumedLearnerId = "154138d0-6cf4-4874-ae46-3f70e52ea3d9"

FOR($curItr = 17000; $curItr -le $itr; $curItr += 1000){

    java -jar thesis-adaptiveEnsemble-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $curItr -r $resumedLearnerId 

    Write-Host "adaptiveEnsemble $curItr of $itr data processed." -ForegroundColor Yellow
    IF($curItr -ne $itr){
        Start-Sleep -s 120 #300
    }
}

Copy-Item "../simulationResults/adaptiveEnsembleData.tex" $env:ThesisExpTex -Force
Copy-Item "../simulationResults/PLTAdaptiveEnsemble" $env:ThesisExpResults -Force -Recurse