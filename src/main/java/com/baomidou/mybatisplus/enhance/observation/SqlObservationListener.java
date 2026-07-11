package com.baomidou.mybatisplus.enhance.observation;

/**
 * Listener invoked after a MyBatis SQL execution completes.
 */
@FunctionalInterface
public interface SqlObservationListener {

    void onCompleted(SqlObservation observation);
}
