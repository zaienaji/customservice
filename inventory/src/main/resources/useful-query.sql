/*
product id yang sudah valid tanpa negatif qty on hand:
A69E62DBDDD44FF7B3A42100A6462641
32D2A76A81584741AF1CFD73F3BAD509
D331AACC8E5F425A9129F530002EA669

product id kandidat untuk divalidasi tanpa negative qty on hand:
3460F69F5A38458892237F73418124A9
59B8B53245464E55991AFDB9604A210B
1D0F5BA13E4443AA981DE3412A9C6067
2F3F205991324A33BA850CD31D32FDEF
 */

--select untuk menghasilkan test data
select	a.m_transaction_id  as "correllationId",
		a.m_product_id as "productCorrelationId",
		'MovingAverage' as "valuationType",
		case when (a.movementtype='C-') then 'CustomerShipment'
			 when (a.movementtype='C+') then 'CustomerReturn'
			 when (a.movementtype='M-') then 'MovementOut'
			 when (a.movementtype='M+') then 'MovementIn'
			 when (a.movementtype='V+') then 'VendorReceipt'
			 when (a.movementtype='V-') then 'VendorReturn'
			 when (a.movementtype in ('I+', 'I-') and a.movementqty<0) then 'PhysicalInventoryOut'
			 when (a.movementtype in ('I+', 'I-') and a.movementqty>=0) then 'PhysicalInventoryIn'
			 else 'Unknown'
			 end as "movementType",
		case when (a.movementtype='C-' and a.movementqty<0) then a.movementqty*(-1)
			 when (a.movementtype='I-' and a.movementqty<0) then a.movementqty*(-1)
			 when (a.movementtype='I+' and a.movementqty<0) then a.movementqty*(-1)
			 when (a.movementtype='M-' and a.movementqty<0) then a.movementqty*(-1)
			 when (a.movementtype='V-' and a.movementqty<0) then a.movementqty*(-1)
			 else a.movementqty
			 end as "movementQuantity",
		case when (a.movementtype='V+' and a.movementqty>=0) then 
				coalesce(
				 (select sum(y.linenetamt) as acquisitionCost 
				 from m_matchinv x
				 left join c_invoiceline y on y.c_invoiceline_id = x.c_invoiceline_id
				 where x.m_inoutline_id = a.m_inoutline_id),
				 (select sum(v.linenetamt) as acquisitionCost
				 from m_matchpo u
				 left join c_orderline v on v.c_orderline_id = u.c_orderline_id
				 where u.m_inoutline_id =a.m_inoutline_id)
				)
			 when (a.movementtype in ('I-', 'I+')) then
			 	(select coalesce(cost, 0::numeric) from m_inventoryline x where x.m_inventoryline_id = a.m_inventoryline_id )
			 end as "acquisitionCost",
		TO_CHAR(a.movementdate :: timestamp, 'yyyy-MM-ddThh:mm') as "movementDate",
		'NotCalculated' as "costingStatus",
		a.m_movementline_id as "movementOutCorrelationId"
from m_transaction a
where a.m_product_id ='A69E62DBDDD44FF7B3A42100A6462641'
order by a.movementdate  asc, a.trxprocessdate asc
