library(RSNNS)
library(data.table)

#CARGA DE DATOS
trainSet <- data.table(read.csv("DatosTrain.csv",dec=".",sep=",",header = T))
validSet <- data.table(read.csv("DatosValid.csv",dec=".",sep=",",header = T))
testSet  <- data.table(read.csv("DatosTest.csv" ,dec=".",sep=",",header = T))
test <- as.matrix(read.csv("DatosTest.csv",dec=".",sep=",",header = T)$salida)

#ELECCION DEL OBJETIVO
target <- "salida"

#SELECCION DE LOS PARAMETROS
topologia        <- c(50) #PARAMETRO DEL TIPO c(A,B,C,...,X) A SIENDO LAS NEURONAS EN LA CAPA OCULTA 1, B LA CAPA 2 ...
razonAprendizaje <- 0.005 #NUMERO REAL ENTRE 0 y 1
ciclosMaximos    <- 9355 #NUMERO ENTERO MAYOR QUE 0

#EJECUCION DEL APRENDIZAJE Y GENERACION DEL MODELO
set.seed(1)
model <- mlp(x= trainSet[,-target,with=F],
             y= trainSet[, target,with=F],
             inputsTest=  validSet[,-target,with=F],
             targetsTest= validSet[, target,with=F],
             size= topologia,
             maxit=ciclosMaximos,
             learnFuncParams=c(razonAprendizaje),
             shufflePatterns = F
             )

#GRAFICO DE LA EVOLUCION DEL ERROR
plotIterativeError(model)
#GRAFICO SALIDAS OBTENIDAS VS DESEADAS
plot(predict(model, testSet[,-target,with=F])*(31254000-906000)+906000,test*(31254000-906000)+906000, col="#FF0066", xlab ="Salidas obtenidas", ylab = "Salidas deseadas")
abline(a = 0, b = 1, col="#3399FF")

#ERROR CUADRATICO MEDIO
MSE <- function(pred,obs) sum((pred-obs)^2)/nrow(obs)

#VECTOR DE LOS ERRORES
errors <- c(TrainRMSE= MSE(pred= predict(model,trainSet[,-target,with=F])*(31254000-906000)+906000,obs= trainSet[,target,with=F]*(31254000-906000)+906000),
            ValidRMSE= MSE(pred= predict(model,validSet[,-target,with=F])*(31254000-906000)+906000,obs= validSet[,target,with=F]*(31254000-906000)+906000),
            TestRMSE=  MSE(pred= predict(model, testSet[,-target,with=F])*(31254000-906000)+906000,obs=  testSet[,target,with=F]*(31254000-906000)+906000))

#TABLA CON LOS ERRORES POR CICLO
iterativeErrors <- data.table(MSETrain= (model$IterativeFitError/ nrow(trainSet)),
                              MSEValid= (model$IterativeTestError/nrow(validSet)))

#SALIDAS DE LA RED
outputs <- data.table(train=   predict(model,trainSet[,-target,with=F]),
                      valid= c(predict(model,validSet[,-target,with=F]),rep(NA,nrow(trainSet)-nrow(validSet))),
                      test=  c(predict(model, testSet[,-target,with=F]),rep(NA,nrow(trainSet)-nrow(testSet ))))
colnames(outputs) <- c("train","validation","test")

#GUARDADO DE RESULTADOS
saveRDS(model,"nnet.rds")
write.csv2(errors,"finalErrors.csv")
write.csv2(iterativeErrors,"iterativeErrors.csv")
write.csv2(outputs,"netOutputs.csv")