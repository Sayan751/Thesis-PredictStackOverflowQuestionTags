cd "D:\GitControlled\MasterThesis\thesis"

$regPath = 'HKCU:\SOFTWARE\JavaSoft\Prefs\thesis'

function run($itr, $af, $epsilon, $nMin, $retainmentFraction){
    Try{
        Write-Host "Configuration confirmed. Running..." -ForegroundColor Green
        java -jar thesis-adaptiveEnsemble-0.0.22-SNAPSHOT-jar-with-dependencies.jar -itr $itr -af $af -ep $epsilon -nm $nMin -ret $retainmentFraction -wreg $true     
    }
    Catch{
        write-host “Caught an exception:” -ForegroundColor Red
        write-host “Exception Type: $($_.Exception.GetType().FullName)” -ForegroundColor Red
        write-host “Exception Message: $($_.Exception.Message)” -ForegroundColor Red
    }
}

function resume($itr, $resumedLearnerId, $wres){
    java -jar thesis-adaptiveEnsemble-0.0.22-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId -wres $wres
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

$retainmentFractions =  0.1, 0.2

    $afStr = Read-Host -Prompt "Enter age function [L(NumberOfLabelsBased), any other key(NumberTrainingInstancesBased)]"
    $af = IF ($afStr.ToLower() -eq "l"){ "NumberOfLabelsBased" } ELSE {"NumberTrainingInstancesBased"}

    $epsilon = [double](Read-Host -Prompt "Enter epsilon (double)")
    IF($epsilon -gt 1){ 
        throw "epsilon is greater than 1"
    }

    $nMin = [int](Read-Host -Prompt "Enter minimum training instances (int)")
    $retainmentFraction = [double](Read-Host -Prompt "Enter retainment fraction (double)")
    IF($retainmentFraction -gt 1){ 
        throw "Retainment fraction is greater than 1"
    }

    Write-Host "Configuration: `n`t Age function: $af,  `n`t Epsilon: $epsilon,  `n`t Minimum training instances: $nMin,  `n`t Retainment fraction: $retainmentFraction"

    $confirm = Read-Host -Prompt "Press 1 to confirm"
    IF($confirm -eq 1){
        $Reg_val = "INIT"
        $Reg_key = "adaptiveEnsemble-$af-$("{0:N6}" -f $epsilon)-$nMin-$("{0:N1}" -f $retainmentFraction)".ToLower() #{0}_{1,number,0.000000}_{2}_{3,number,0.0}
        
        Write-Host $Reg_key

        if([string]::IsNullOrWhiteSpace($Reg_key)){
            throw "Invalid config : Reg key NULL"
        }

        AddToRegistry $Reg_key $Reg_val 

        $chunkSize = 1000
        $totalCount = 25000
        $resumedLearnerId = ""

        FOR ($chunk = $chunkSize; $chunk -le $totalCount; $chunk += $chunkSize){
            $resumedLearnerId = ReadFromRegistry $Reg_key
            Write-Host $resumedLearnerId

            #first chunk
            IF($chunk -eq $chunkSize){                
                run $chunk $af $epsilon $nMin $retainmentFraction
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

        Copy-Item "../simulationResults/adaptiveEnsembleData.tex" $env:ThesisExpTex -Force
        Copy-Item "../simulationResults/PLTAdaptiveEnsemble" $env:ThesisExpResults -Force -Recurse
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