cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
$resumedLearnerId = "ea6e4235-9022-43e7-b7f4-1285a29c2df5"

FOR($curItr = 7700; $curItr -le $itr; $curItr += 100){

    java -jar thesis-boostedEnsemble-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $curItr -r $resumedLearnerId

    Write-Host "boostedEnsemble $curItr of $itr data processed." -ForegroundColor Yellow
    IF($curItr -ne $itr){
        Start-Sleep -s 120 #300
    }
}

Copy-Item "../simulationResults/ensembleBoostingData.tex" $env:ThesisExpTex -Force
Copy-Item "../simulationResults/PLTEnsembleBoosted" $env:ThesisExpResults -Force -Recurse