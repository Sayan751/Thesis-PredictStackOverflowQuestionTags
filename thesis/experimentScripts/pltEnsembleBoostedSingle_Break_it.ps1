cd "D:\GitControlled\MasterThesis\thesis"

$regPath = 'HKCU:\SOFTWARE\JavaSoft\Prefs\thesis'

function run($itr, $hd, $es, $aggrPref, $minEpoch){
    Try{
        Write-Host "Configuration confirmed. Running..." -ForegroundColor Green
        java -jar thesis-boostedEnsemble-0.0.22-SNAPSHOT-jar-with-dependencies.jar -itr $itr -hd $hd -es $es -mv $aggrPref -me $minEpoch -wreg $true
        
    }
    Catch{
        write-host “Caught an exception:” -ForegroundColor Red
        write-host “Exception Type: $($_.Exception.GetType().FullName)” -ForegroundColor Red
        write-host “Exception Message: $($_.Exception.Message)” -ForegroundColor Red
    }
}

function resume($itr, $resumedLearnerId, $wres){
    java -jar thesis-boostedEnsemble-0.0.22-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId -wres $wres
}

function AddToRegistry($Reg_key,$Reg_val){
        if(! (Test-Path $regPath)){
            New-Item -Path $regPath -Force | Out-Null
        }
        New-ItemProperty -Path $regPath -Name $Reg_key -Value $Reg_val -Force | Out-Null
}

function ReadFromRegistry($Reg_key){
        $LearnerID = Get-ItemPropertyValue -Path $regPath -Name "$Reg_key"
        return $LearnerID
}


Write-Host "STARTING APPLICATION..." -ForegroundColor Yellow


Try{

    $hd = [int](Read-Host -Prompt "Enter hashed dimension (int)")
    $es = [int](Read-Host -Prompt "Enter ensemble size (int)")
    $aggrPrefStr = Read-Host -Prompt "Aggregate by majority vote? [1(true), any other key(false)]"
    $aggrPref = $aggrPrefStr -eq 1
    $minEpoch = [int](Read-Host -Prompt "Enter min epoch (int)")

    Write-Host "Configuration: `n`tHashed Dimension: $hd,  `n`tEnsemble size: $es,  `n`tAggregate by majority vote?: $aggrPref,  `n`tMinimum epoch: $minEpoch"

    $confirm = Read-Host -Prompt "Press 1 to confirm"
    IF($confirm -eq 1){
        $Reg_val = "INIT"
        $Reg_key = "boostedEnsemble-$hd-$es-$aggrPref-$minEpoch".ToLower()
        
        Write-Host $Reg_key

        if([string]::IsNullOrWhiteSpace($Reg_key)){
            throw "Invalid config : Reg key NULL"
        }

        AddToRegistry $Reg_key $Reg_val 

        $chunkSize = 100
        $totalCount = 25000
        $resumedLearnerId = ""

        FOR ($chunk = $chunkSize; $chunk -le $totalCount; $chunk += $chunkSize){
            $resumedLearnerId = ReadFromRegistry $Reg_key
            Write-Host $resumedLearnerId

            #first chunk
            IF($chunk -eq $chunkSize){                
                run $chunk $hd $es $aggrPref $minEpoch
            }
            #last chunk
            ELSEIF($chunk -eq $totalCount){
                resume ([math]::Min($chunk, $totalCount)) $resumedLearnerId $true
            }
            #Chunks in the middle
            ELSE{               
                resume $chunk $resumedLearnerId $false
            }

            Write-Host "$Reg_key - Phase $($chunk/$chunkSize) over; $([math]::Min($chunk, $totalCount)) data processed." -ForegroundColor Yellow
            IF($chunk -ne $totalCount){
                Start-Sleep -s 120
            }
        }

        Copy-Item "../simulationResults/ensembleBoostingData.tex" $env:ThesisExpTex -Force
        Copy-Item "../simulationResults/PLTEnsembleBoosted" $env:ThesisExpResults -Force -Recurse
    }
    ELSE{
        Write-Host "Configuration not confirmed. Aborted." -ForegroundColor Yellow
    }
}
Catch{
    write-host “Caught an exception:” -ForegroundColor Red
    write-host “Exception Type: $($_.Exception.GetType().FullName)” -ForegroundColor Red
    write-host “Exception Message: $($_.Exception.Message)” -ForegroundColor Red
}

Write-Host "ENDING APPLICATION.." -ForegroundColor Yellow