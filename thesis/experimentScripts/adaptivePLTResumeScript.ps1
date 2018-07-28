cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
$resumedLearnerId = "d5852b58-36a7-48bf-8087-63b116a9ccba"

java -jar thesis-adaptivePlt-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId

Copy-Item "../simulationResults/adaptivePLTData.tex" $env:ThesisExpTex -Force
Copy-Item "../simulationResults/AdaptivePLT" $env:ThesisExpResults -Force -Recurse