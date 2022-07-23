import pandas as pd
import numpy as np
import json
import math

class Product(object):
    def __init__(self, correlationId: str, valuationType: str):
        self.correlationId = correlationId
        self.valuationType = valuationType
        
class MaterialTransaction(object):
    def __init__(self, correlationId: str, product: Product):
        self.correlationId = correlationId
        self.product = product

filename = 'm_transaction.csv'
df = pd.read_csv(filename)

mTransactions = []
for index, row in df.iterrows():
    product = Product(correlationId=row['productCorrelationId'], valuationType=row['valuationType'])
    
    mTransaction = MaterialTransaction(correlationId=row['correllationId'], product=product)
    mTransaction.movementType = row['movementType']
    mTransaction.movementQuantity = row['movementQuantity']
    mTransaction.acquisitionCost = None if math.isnan(row['acquisitionCost']) else row['acquisitionCost']
    mTransaction.movementDate = row['movementDate']
    mTransaction.costingStatus = row['costingStatus']
    
    if not (row['movementOutCorrelationId'] is np.nan):
        mTransaction.movementOutCorrelationId = row['movementOutCorrelationId']
    
    mTransactions.append(mTransaction)

jsonStr = json.dumps(mTransactions, default=lambda o: o.__dict__, indent=4)
print(jsonStr)