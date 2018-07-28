cd "D:\GitControlled\MasterThesis\thesis"

$itr = 25000
Try{
    $hd = [int](Read-Host -Prompt "Enter hashed dimension (int)")
    $alpha = [double](Read-Host -Prompt "Enter alpha (double)")

    IF($alpha -gt 1){ 
        throw "Alpha is greater than 1"
    }

    $prefStr = Read-Host -Prompt "Enter 'to prefer Node With Highest Probability' [1(true), anything other key(false)]"
    $pref = $prefStr -eq 1
    Write-Host "Configuration: `n`tHashed Dimension: $($hd),  `n`tAlpha: $($alpha),  `n`tPrefer node with highest prob: $($pref)"

    $confirm = Read-Host -Prompt "Press 1 to confirm"
    IF($confirm -eq 1){
        Write-Host "Configuration confirmed. Running..." -ForegroundColor Green
        java -jar thesis-adaptivePlt-0.0.13-SNAPSHOT-jar-with-dependencies.jar -itr $itr -hd $hd -alpha $alpha -hp $pref

        Copy-Item "../simulationResults/adaptivePLTData.tex" $env:ThesisExpTex -Force
        Copy-Item "../simulationResults/AdaptivePLT" $env:ThesisExpResults -Force -Recurse
    }ELSE{
        Write-Host "Configuration not confirmed. Aborted." -ForegroundColor Yellow
    }
}Catch{
    write-host “Caught an exception:” -ForegroundColor Red
    write-host “Exception Type: $($_.Exception.GetType().FullName)” -ForegroundColor Red
    write-host “Exception Message: $($_.Exception.Message)” -ForegroundColor Red
}